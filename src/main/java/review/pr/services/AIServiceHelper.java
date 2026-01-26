package review.pr.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import review.pr.exception.general.MissingPullRequestDataException;
import review.pr.exception.general.UnsupportedGitHubEventException;

import java.util.Map;

import static review.pr.constants.AppConstants.*;

/**
 * Service for orchestrating the PR review workflow.
 * Delegates to specialized services for diff fetching, AI analysis, and GitHub API calls.
 */
@Service
@Slf4j
public class AIServiceHelper {

    private final DiffService diffService;
    private final AIService aiService;
    private final GitHubService gitHubService;

    public AIServiceHelper(DiffService diffService, AIService aiService, GitHubService gitHubService) {
        this.diffService = diffService;
        this.aiService = aiService;
        this.gitHubService = gitHubService;
    }

    public String reviewPR(Map<String, Object> payload, Map<String, String> headers) {
        String event = headers.get(X_GITHUB_EVENT);
        log.info("Received GitHub event: {}", event);

        if (PULL_REQUEST.equals(event)) {
            String action = (String) payload.get(ACTION);
            log.info("Pull request action: {}", action);

            if (OPENED.equals(action)) {
                return processOpenedPullRequest(payload);
            } else {
                log.info("Ignoring pull request action: {}", action);
                return PR_CLOSED_MESSAGE;
            }
        } else {
            log.warn("Unsupported GitHub event: {}", event);
            throw new UnsupportedGitHubEventException(UNKNOWN_EVENT);
        }
    }

    private String processOpenedPullRequest(Map<String, Object> payload) {
        // Extract diff URL from payload
        Map<String, Object> pullRequest = (Map<String, Object>) payload.get(PULL_REQUEST);

        if (pullRequest == null || !pullRequest.containsKey(DIFF_URL)) {
            log.error("Pull request data or diff_url not found in payload");
            throw new MissingPullRequestDataException(FAILURE_MESSAGE);
        }

        String diffUrl = (String) pullRequest.get(DIFF_URL);
        log.info("Processing pull request with diff URL: {}", maskUrl(diffUrl));

        // Fetch diff content
        String diffContent = diffService.fetchDiff(diffUrl);

        // Get AI review
        String review = aiService.chat(diffContent);

        // Parse review into map of filename -> comment
        Map<String, String> reviews = aiService.parseReview(review);

        // Get comment positions
        Map<String, Integer> commentPositions = diffService.computeCommentPositions(diffContent);

        // Post review comments
        postReviewComments(payload, reviews, commentPositions);

        return PR_OPENED_MESSAGE;
    }

    private void postReviewComments(Map<String, Object> payload, Map<String, String> reviews, Map<String, Integer> commentPositions) {
        Map<String, Object> pullRequest = (Map<String, Object>) payload.get(PULL_REQUEST);
        Map<String, Object> head = (Map<String, Object>) pullRequest.get(HEAD);
        String commitId = (String) head.get(SHA);
        String repoUrl = (String) pullRequest.get(URL);
        String commentsUrl = repoUrl + COMMENTS_ENDPOINT;

        int postedCount = 0;
        for (Map.Entry<String, String> entry : reviews.entrySet()) {
            String fileName = entry.getKey();
            String comment = entry.getValue();
            Integer position = commentPositions.get(fileName);

            if (position != null) {
                gitHubService.postReviewComment(commentsUrl, comment, fileName, position, commitId);
                postedCount++;
            } else {
                log.warn("No position found for file: {}, skipping comment", fileName);
            }
        }

        log.info("Posted {} review comments", postedCount);
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("api\\.github\\.com/repos/[^/]+/[^/]+", "api.github.com/repos/***/***");
    }
}
