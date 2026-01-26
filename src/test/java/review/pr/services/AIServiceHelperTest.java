package review.pr.services;

import review.pr.exception.general.MissingPullRequestDataException;
import review.pr.exception.general.UnsupportedGitHubEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIServiceHelperTest {

    @Mock
    private DiffService diffService;

    @Mock
    private AIService aiService;

    @Mock
    private GitHubService gitHubService;

    private AIServiceHelper aiServiceHelper;

    @BeforeEach
    void setUp() {
        aiServiceHelper = new AIServiceHelper(diffService, aiService, gitHubService);
    }

    @Test
    void reviewPR_OpenedEvent_Success() {
        // Arrange
        Map<String, Object> payload = createValidPayload();
        Map<String, String> headers = createHeaders("pull_request");

        String diffContent = "diff content";
        String aiReview = "{\"file.java\": \"Review comment\"}";
        Map<String, String> parsedReviews = Map.of("file.java", "Review comment");
        Map<String, Integer> commentPositions = Map.of("file.java", 5);

        when(diffService.fetchDiff(anyString())).thenReturn(diffContent);
        when(aiService.chat(diffContent)).thenReturn(aiReview);
        when(aiService.parseReview(aiReview)).thenReturn(parsedReviews);
        when(diffService.computeCommentPositions(diffContent)).thenReturn(commentPositions);

        // Act
        String result = aiServiceHelper.reviewPR(payload, headers);

        // Assert
        assertEquals("Pull request opened. Reviews Posted", result);
        verify(diffService).fetchDiff(anyString());
        verify(aiService).chat(diffContent);
        verify(aiService).parseReview(aiReview);
        verify(diffService).computeCommentPositions(diffContent);
        verify(gitHubService).postReviewComment(anyString(), eq("Review comment"), eq("file.java"), eq(5), anyString());
    }

    @Test
    void reviewPR_ClosedEvent_ReturnsClosedMessage() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "closed");
        Map<String, String> headers = createHeaders("pull_request");

        // Act
        String result = aiServiceHelper.reviewPR(payload, headers);

        // Assert
        assertEquals("Pull Request Closed", result);
        verify(diffService, never()).fetchDiff(anyString());
        verify(aiService, never()).chat(anyString());
    }

    @Test
    void reviewPR_UnsupportedEvent_ThrowsUnsupportedGitHubEventException() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> headers = createHeaders("push");

        // Act & Assert
        assertThrows(UnsupportedGitHubEventException.class, () -> aiServiceHelper.reviewPR(payload, headers));
    }

    @Test
    void reviewPR_MissingPullRequestData_ReturnsClosedMessage() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> headers = createHeaders("pull_request");

        // Act
        String result = aiServiceHelper.reviewPR(payload, headers);

        // Assert
        assertEquals("Pull Request Closed", result);
    }

    @Test
    void reviewPR_NoDiffUrl_ReturnsClosedMessage() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> pullRequest = new HashMap<>();
        payload.put("pull_request", pullRequest);
        Map<String, String> headers = createHeaders("pull_request");

        // Act
        String result = aiServiceHelper.reviewPR(payload, headers);

        // Assert
        assertEquals("Pull Request Closed", result);
    }

    @Test
    void reviewPR_NoPositionForFile_SkipsComment() {
        // Arrange
        Map<String, Object> payload = createValidPayload();
        Map<String, String> headers = createHeaders("pull_request");

        String diffContent = "diff content";
        String aiReview = "{\"file.java\": \"Review comment\"}";
        Map<String, String> parsedReviews = Map.of("file.java", "Review comment");
        Map<String, Integer> commentPositions = new HashMap<>(); // Empty positions

        when(diffService.fetchDiff(anyString())).thenReturn(diffContent);
        when(aiService.chat(diffContent)).thenReturn(aiReview);
        when(aiService.parseReview(aiReview)).thenReturn(parsedReviews);
        when(diffService.computeCommentPositions(diffContent)).thenReturn(commentPositions);

        // Act
        String result = aiServiceHelper.reviewPR(payload, headers);

        // Assert
        assertEquals("Pull request opened. Reviews Posted", result);
        verify(gitHubService, never()).postReviewComment(anyString(), anyString(), anyString(), any(), anyString());
    }

    private Map<String, Object> createValidPayload() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> pullRequest = new HashMap<>();
        Map<String, Object> head = new HashMap<>();

        head.put("sha", "abc123");
        pullRequest.put("url", "https://api.github.com/repos/test/test/pulls/1");
        pullRequest.put("diff_url", "https://github.com/repos/test/test/pulls/1.diff");
        pullRequest.put("head", head);
        payload.put("pull_request", pullRequest);
        payload.put("action", "opened");

        return payload;
    }

    private Map<String, String> createHeaders(String event) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-github-event", event);
        return headers;
    }
}
