package review.pr.services;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static review.pr.constants.AppConstants.*;

@Service
@Slf4j
public class AIServiceHelper {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${github-access-token}")
    private String github_access_token;
    @Autowired
    private final ChatClient chatClient;

    public AIServiceHelper(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(String question){
        return this.chatClient.prompt()
                .user(INITIAL_PROMPT + question)
                .call()
                .content();
    }

    public String reviewPR(Map<String, Object> payload, Map<String, String> headers){
        String event = headers.get(X_GITHUB_EVENT);
        System.out.println("EVENT: "+ event );
        if (PULL_REQUEST.equals(event)) {
            String action = (String) payload.get(ACTION);

            if (OPENED.equals(action)) {
                // Extract diff URL from payload
                Map<String, Object> pullRequest = (Map<String, Object>) payload.get(PULL_REQUEST);

                if (pullRequest != null && pullRequest.containsKey(DIFF_URL)) {
                    String diffUrl = (String) pullRequest.get(DIFF_URL);

                    // Fetch diff content
                    String diffContent = fetchDiff(diffUrl);

                    // Process the diff content for reviewing here...
                    String review = chat(diffContent);

                    Map<String,String> reviews = parseReview(review);
                    for (Map.Entry<String, String> entry : reviews.entrySet()) {
                        String fileName = entry.getKey();
                        String comment = entry.getValue();

                        postReviewComment(payload, comment, fileName);
                    }
                    return PR_OPENED_MESSAGE;
                }
                else {
                    return FAILURE_MESSAGE;
                }
            } else {
                return PR_CLOSED_MESSAGE;
            }
        } else {
            return UNKNOWN_EVENT;
        }
    }
    private String fetchDiff(String diffUrl) {
        try {
            return restTemplate.getForObject(diffUrl, String.class);
        } catch (Exception e) {
            return DIFF_FETCH_FAIL_MESSAGE + e.getMessage();
        }
    }

    private Map<String, String> parseReview(String reviewJson) {
        Map<String, String> reviews = new HashMap<>();

        try {
            // Strip Markdown-style code block if present
            if (reviewJson.startsWith(JSON_CHECK)) {
                reviewJson = reviewJson.replaceAll(JSON_REGEX, JSON_REPLACEMENT);
            }
            JSONObject jsonObject = new JSONObject(reviewJson);
            for (String fileName : jsonObject.keySet()) {
                reviews.put(fileName, jsonObject.getString(fileName));
            }
        } catch (Exception e) {
            System.err.println(INVALID_JSON + reviewJson);
            e.printStackTrace();
        }
        return reviews;
    }

    private void postReviewComment(Map<String,Object> payload, String review, String filepath){
        Map<String, Object> pullRequest = (Map<String, Object>) payload.get(PULL_REQUEST);
        Map<String, Object> head = (Map<String, Object>) pullRequest.get(HEAD);
        String commitId = (String) head.get(SHA);

        String repoUrl = (String) pullRequest.get(URL);
        String commentsUrl = repoUrl + COMMENTS_ENDPOINT;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AUTHORIZATION, BEARER +  github_access_token);
        headers.set(ACCEPT, ACCEPT_VALUE);

        Map<String, Object> commentBody = Map.of(
                BODY, review,
                COMMIT_ID, commitId,
                PATH, filepath,
                POSITION, 1
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(commentBody, headers);
        try {
            restTemplate.postForEntity(commentsUrl, request, String.class);
        } catch (Exception e) {
            System.err.println(GITHUB_API_FAILURE + e.getMessage());
        }
    }
}