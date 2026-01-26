package review.pr.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import review.pr.services.AIServiceHelper;

import java.util.Map;

import static review.pr.constants.AppConstants.*;

@RestController
@RequestMapping(BASE_ENDPOINT)
@Tag(name = "PR Review", description = "API for automated pull request code review")
@Slf4j
public class SpringAIController {

    @Autowired
    private AIServiceHelper aiServiceHelper;

    @PostMapping(REVIEW_ENDPOINT)
    @Operation(
        summary = "Review a pull request",
        description = "Webhook endpoint that receives GitHub PR events and triggers automated code review using AI"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Review processed successfully",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or unsupported event"
        ),
        @ApiResponse(
            responseCode = "502",
            description = "Failed to fetch diff or post review comment"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Invalid JSON received from AI service"
        )
    })
    public ResponseEntity<String> reviewPR(
            @Parameter(description = "GitHub webhook payload containing PR information")
            @RequestBody Map<String, Object> payload,
            @Parameter(description = "HTTP headers including GitHub event type")
            @RequestHeader Map<String, String> headers) {

        log.info("Received webhook request");

        // Validate required fields
        String event = headers.get(X_GITHUB_EVENT);
        if (event == null || event.isEmpty()) {
            log.error("Missing x-github-event header");
            return ResponseEntity.badRequest().body("Missing x-github-event header");
        }

        if (payload == null || payload.isEmpty()) {
            log.error("Empty payload received");
            return ResponseEntity.badRequest().body("Empty payload");
        }

        String response = aiServiceHelper.reviewPR(payload, headers);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("PR Reviewer is running");
    }
}
