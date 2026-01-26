package review.pr.services;

import review.pr.exception.general.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        gitHubService = new GitHubService(restTemplate);
        ReflectionTestUtils.setField(gitHubService, "githubAccessToken", "test-token");
        ReflectionTestUtils.setField(gitHubService, "maxRequestsPerHour", 100);
        gitHubService.resetRateLimit();
    }

    @Test
    void postReviewComment_Success() {
        // Arrange
        String commentsUrl = "https://api.github.com/repos/test/test/pulls/1/comments";
        String review = "This is a review comment";
        String filepath = "src/main.java";
        Integer position = 5;
        String commitId = "abc123";

        when(restTemplate.postForEntity(
                eq(commentsUrl),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act
        assertDoesNotThrow(() -> gitHubService.postReviewComment(commentsUrl, review, filepath, position, commitId));

        // Assert
        verify(restTemplate).postForEntity(eq(commentsUrl), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void postReviewComment_RestClientException_ThrowsGitHubApiException() {
        // Arrange
        String commentsUrl = "https://api.github.com/repos/test/test/pulls/1/comments";
        String review = "This is a review comment";
        String filepath = "src/main.java";
        Integer position = 5;
        String commitId = "abc123";

        lenient().when(restTemplate.postForEntity(
                eq(commentsUrl),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RestClientException("API error"));

        // Act & Assert
        assertThrows(GitHubApiException.class,
                () -> gitHubService.postReviewComment(commentsUrl, review, filepath, position, commitId));
    }

    @Test
    void updateRateLimit_Success() {
        // Act
        assertDoesNotThrow(() -> gitHubService.updateRateLimit(500));
        assertEquals(500, gitHubService.getMaxRequestsPerHour());
    }

    @Test
    void resetRateLimit_ResetsCounterToZero() {
        // Act
        gitHubService.resetRateLimit();

        // Assert
        assertEquals(0, gitHubService.getRequestCount());
    }

    @Test
    void getRequestCount_ReturnsCurrentCount() {
        // Act
        long count = gitHubService.getRequestCount();

        // Assert
        assertEquals(0, count);
    }
}
