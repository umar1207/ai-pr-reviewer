package review.pr.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import review.pr.exception.general.InvalidReviewJsonException;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AIService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${pr.reviewer.initial-prompt:You are a PR Code Reviewer. Respond with a JSON object like {\\\"filename\\\": \\\"review comment\\\"}. Here's the git diff:\\n}")
    private String initialPrompt;

    public AIService(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String chat(String question) {
        log.info("Sending request to AI for code review (diff length: {})", question != null ? question.length() : 0);
        try {
            String response = this.chatClient.prompt()
                    .user(initialPrompt + question)
                    .call()
                    .content();
            log.info("Received AI response (length: {})", response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw e;
        }
    }

    public Map<String, String> parseReview(String reviewJson) {
        log.debug("Parsing AI review response");
        Map<String, String> reviews = new HashMap<>();

        if (reviewJson == null || reviewJson.isEmpty()) {
            log.warn("Empty review response received");
            throw new InvalidReviewJsonException("Empty review response received");
        }

        try {
            // Strip Markdown-style code block if present
            String cleanedJson = reviewJson;
            if (reviewJson.startsWith("```json")) {
                cleanedJson = reviewJson.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1");
                log.debug("Stripped markdown code block from JSON response");
            }

            // Trim whitespace
            cleanedJson = cleanedJson.trim();

            // Parse JSON using ObjectMapper for better error handling
            Map<String, String> parsedReviews = objectMapper.readValue(cleanedJson, Map.class);
            reviews.putAll(parsedReviews);

            log.info("Successfully parsed {} review comments", reviews.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse review JSON", e);
            throw new InvalidReviewJsonException("Invalid JSON received: " + e.getMessage());
        }

        return reviews;
    }
}
