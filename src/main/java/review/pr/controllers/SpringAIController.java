package review.pr.controllers;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import review.pr.services.AIServiceHelper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SpringAIController {
    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private AIServiceHelper aiServiceHelper;

    @PostMapping("/review/pr")
    public ResponseEntity<String> reviewPR(
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {

        String event = headers.get("x-github-event");
        if ("push".equals(event)) {
            return ResponseEntity.ok("push");
        } else if ("pull_request".equals(event)) {
            String action = (String) payload.get("action");

            if ("opened".equals(action)) {
                // Extract diff URL from payload
                Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");

                if (pullRequest != null && pullRequest.containsKey("diff_url")) {
                    String diffUrl = (String) pullRequest.get("diff_url");

                    // Fetch diff content
                    String diffContent = fetchDiff(diffUrl);

                    // Process the diff content for reviewing here...
                    String review = aiServiceHelper.chat(diffContent);

                    Map<String,String> reviews = parseReview(review);
                    for (Map.Entry<String, String> entry : reviews.entrySet()) {
                        String fileName = entry.getKey();
                        String comment = entry.getValue();

                        postReviewComment(payload, comment, fileName);
                    }

                    return ResponseEntity.ok("Pull request opened. Reviews Posted");
                }
                return ResponseEntity.ok("Pull request opened but no diff_url found.");
            } else {
                return ResponseEntity.ok("Pull Request Closed");
            }
        } else {
            return ResponseEntity.ok("unknown");
        }
    }
    private String fetchDiff(String diffUrl) {
        try {
            return restTemplate.getForObject(diffUrl, String.class);
        } catch (Exception e) {
            return "Failed to fetch diff: " + e.getMessage();
        }
    }

    private Map<String, String> parseReview(String reviewJson) {
        Map<String, String> reviews = new HashMap<>();

        try {
            // Strip Markdown-style code block if present
            if (reviewJson.startsWith("```json")) {
                reviewJson = reviewJson.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1");
            }

            JSONObject jsonObject = new JSONObject(reviewJson);
            for (String fileName : jsonObject.keySet()) {
                reviews.put(fileName, jsonObject.getString(fileName));
            }
        } catch (Exception e) {
            System.err.println("Invalid JSON received: " + reviewJson);
            e.printStackTrace();
        }

        return reviews;
    }


    private void postReviewComment(Map<String,Object> payload, String review, String filepath){
        Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
        Map<String, Object> head = (Map<String, Object>) pullRequest.get("head");
        String commitId = (String) head.get("sha");

        String repoUrl = (String) pullRequest.get("url");
        String commentsUrl = repoUrl + "/comments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer token");
        headers.set("Accept", "application/vnd.github+json");

        Map<String, Object> commentBody = Map.of(
                "body", review,
                "commit_id", commitId,
                "path", filepath,
                "position", 1
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(commentBody, headers);

        try {
            restTemplate.postForEntity(commentsUrl, request, String.class);
        } catch (Exception e) {
            System.err.println("Failed to post review comment: " + e.getMessage());
        }

    }
}
