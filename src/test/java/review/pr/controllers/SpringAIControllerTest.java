package review.pr.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import review.pr.services.AIServiceHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpringAIController.class)
class SpringAIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AIServiceHelper aiServiceHelper;

    @Test
    void reviewPR_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> pullRequest = new HashMap<>();
        pullRequest.put("diff_url", "https://github.com/test/test/pull/1.diff");
        payload.put("pull_request", pullRequest);
        payload.put("action", "opened");

        when(aiServiceHelper.reviewPR(any(), any())).thenReturn("Pull request opened. Reviews Posted");

        // Act & Assert
        mockMvc.perform(post("/api/v1/review/pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("x-github-event", "pull_request"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pull request opened. Reviews Posted"));
    }

    @Test
    void reviewPR_MissingEventHeader_ReturnsBadRequest() throws Exception {
        // Arrange
        Map<String, Object> payload = new HashMap<>();

        // Act & Assert
        mockMvc.perform(post("/api/v1/review/pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing x-github-event header"));
    }

    @Test
    void reviewPR_EmptyPayload_ReturnsBadRequest() throws Exception {
        // Act & Assert - Empty JSON object
        mockMvc.perform(post("/api/v1/review/pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("x-github-event", "pull_request"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("Empty payload")));
    }

    @Test
    void health_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("PR Reviewer is running"));
    }
}
