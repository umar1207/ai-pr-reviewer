package review.pr.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import review.pr.exception.general.InvalidReviewJsonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private ChatClient chatClient;

    private AIService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AIService(chatClient, new ObjectMapper());
        ReflectionTestUtils.setField(aiService, "initialPrompt", "Test prompt: ");
    }

    @Test
    void parseReview_ValidJson_ReturnsCorrectMap() {
        // Arrange
        String reviewJson = "{\"file1.java\": \"Review comment 1\", \"file2.java\": \"Review comment 2\"}";

        // Act
        Map<String, String> result = aiService.parseReview(reviewJson);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Review comment 1", result.get("file1.java"));
        assertEquals("Review comment 2", result.get("file2.java"));
    }

    @Test
    void parseReview_JsonWithMarkdown_StripsMarkdownAndParses() {
        // Arrange
        String reviewJson = "```json\n{\"file1.java\": \"Review comment 1\"}\n```";

        // Act
        Map<String, String> result = aiService.parseReview(reviewJson);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Review comment 1", result.get("file1.java"));
    }

    @Test
    void parseReview_EmptyJson_ThrowsInvalidReviewJsonException() {
        // Arrange
        String reviewJson = "";

        // Act & Assert
        assertThrows(InvalidReviewJsonException.class, () -> aiService.parseReview(reviewJson));
    }

    @Test
    void parseReview_NullJson_ThrowsInvalidReviewJsonException() {
        // Act & Assert
        assertThrows(InvalidReviewJsonException.class, () -> aiService.parseReview(null));
    }

    @Test
    void parseReview_InvalidJson_ThrowsInvalidReviewJsonException() {
        // Arrange
        String reviewJson = "This is not valid JSON";

        // Act & Assert
        assertThrows(InvalidReviewJsonException.class, () -> aiService.parseReview(reviewJson));
    }

    @Test
    void parseReview_EmptyJsonObject_ReturnsEmptyMap() {
        // Arrange
        String reviewJson = "{}";

        // Act
        Map<String, String> result = aiService.parseReview(reviewJson);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseReview_JsonWithSpecialCharacters_ParsesCorrectly() {
        // Arrange
        String reviewJson = "{\"file.java\": \"Review with \\\"quotes\\\" and \\n newlines\"}";

        // Act
        Map<String, String> result = aiService.parseReview(reviewJson);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Review with \"quotes\" and \n newlines", result.get("file.java"));
    }
}
