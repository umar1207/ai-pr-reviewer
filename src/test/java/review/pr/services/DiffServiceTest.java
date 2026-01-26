package review.pr.services;

import review.pr.exception.general.DiffFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiffServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private DiffService diffService;

    @BeforeEach
    void setUp() {
        diffService = new DiffService(restTemplate);
        ReflectionTestUtils.setField(diffService, "githubAccessToken", "test-token");
    }

    @Test
    void fetchDiff_Success() {
        // Arrange
        String diffUrl = "https://api.github.com/repos/test/test/pulls/1";
        String expectedDiff = "diff --git a/file.txt b/file.txt\n+++ b/file.txt\n@@ -1,1 +1,2 @@\n-old\n+new";
        when(restTemplate.exchange(
                eq(diffUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(expectedDiff, HttpStatus.OK));

        // Act
        String result = diffService.fetchDiff(diffUrl);

        // Assert
        assertEquals(expectedDiff, result);
        verify(restTemplate).exchange(eq(diffUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void fetchDiff_RestClientException_ThrowsDiffFetchException() {
        // Arrange
        String diffUrl = "https://api.github.com/repos/test/test/pulls/1";
        when(restTemplate.exchange(
                eq(diffUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RestClientException("Connection error"));

        // Act & Assert
        assertThrows(DiffFetchException.class, () -> diffService.fetchDiff(diffUrl));
    }

    @Test
    void computeCommentPositions_ValidDiff_ReturnsCorrectPositions() {
        // Arrange
        String diff = """
                diff --git a/src/main.java b/src/main.java
                +++ b/src/main.java
                @@ -1,3 +1,4 @@
                 import java.util.*;
                +public class Test {
                -private String name;
                +private String newName;
                """;

        // Act
        Map<String, Integer> result = diffService.computeCommentPositions(diff);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("src/main.java"));
        assertTrue(result.get("src/main.java") > 0);
    }

    @Test
    void computeCommentPositions_EmptyDiff_ReturnsEmptyMap() {
        // Arrange
        String diff = "";

        // Act
        Map<String, Integer> result = diffService.computeCommentPositions(diff);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void computeCommentPositions_NullDiff_ReturnsEmptyMap() {
        // Act
        Map<String, Integer> result = diffService.computeCommentPositions(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void computeCommentPositions_MultipleFiles_ReturnsCorrectPositions() {
        // Arrange
        String diff = """
                diff --git a/file1.java b/file1.java
                +++ b/file1.java
                @@ -1,1 +1,2 @@
                -old1
                +new1
                diff --git a/file2.java b/file2.java
                +++ b/file2.java
                @@ -1,1 +1,2 @@
                -old2
                +new2
                """;

        // Act
        Map<String, Integer> result = diffService.computeCommentPositions(diff);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("file1.java"));
        assertTrue(result.containsKey("file2.java"));
    }
}
