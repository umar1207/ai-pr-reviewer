package review.pr.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import review.pr.exception.general.GitHubApiException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate;
    private final AtomicLong requestCount = new AtomicLong(0);

    @Value("${github-access-token}")
    private String githubAccessToken;

    @Value("${github.api.rate-limit:100}")
    private int maxRequestsPerHour;

    public GitHubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
        value = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void postReviewComment(String commentsUrl, String review, String filepath, Integer position, String commitId) {
        // Simple rate limiting - check if we've exceeded the limit
        long currentCount = requestCount.get();
        if (currentCount >= maxRequestsPerHour) {
            log.warn("Rate limit exceeded for GitHub API. Requests: {}/{}", currentCount, maxRequestsPerHour);
            throw new GitHubApiException("Rate limit exceeded. Please try again later.");
        }

        log.info("Posting review comment for file: {} at position: {}", filepath, position);

        // Log comment before posting
        logReviewCommentToFile(commentsUrl, filepath, review, position, commitId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + githubAccessToken);
        headers.set("Accept", "application/vnd.github+json");

        Map<String, Object> commentBody = Map.of(
                "body", review,
                "commit_id", commitId,
                "path", filepath,
                "position", position
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(commentBody, headers);
        try {
            restTemplate.postForEntity(commentsUrl, request, String.class);
            requestCount.incrementAndGet();
            log.info("Successfully posted review comment for file: {}. Total requests: {}", filepath, requestCount.get());
        } catch (RestClientException e) {
            log.error("Failed to post review comment for file: {}", filepath, e);
            throw new GitHubApiException("Failed to post review comment: " + e.getMessage());
        }
    }

    /**
     * Logs review comments to a file for debugging/auditing purposes
     */
    private void logReviewCommentToFile(String commentsUrl, String filepath, String review, Integer position, String commitId) {
        try {
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            Path logFile = logDir.resolve("comments.log");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            String logEntry = String.format(
                "[%s] Comment for: %s | Position: %d | Commit: %s%nReview: %s%nURL: %s%n================================================================================%n",
                timestamp, filepath, position, commitId, review, commentsUrl
            );

            try (FileWriter writer = new FileWriter(logFile.toFile(), true)) {
                writer.write(logEntry);
            }

            log.debug("Logged review comment to comments.log");
        } catch (IOException e) {
            log.warn("Failed to write review comment to log file: {}", e.getMessage());
        }
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public void resetRateLimit() {
        log.info("Resetting rate limit counter");
        requestCount.set(0);
    }

    public int getMaxRequestsPerHour() {
        return maxRequestsPerHour;
    }

    public void updateRateLimit(int newLimit) {
        log.info("Updating rate limit to: {}", newLimit);
        this.maxRequestsPerHour = newLimit;
    }
}
