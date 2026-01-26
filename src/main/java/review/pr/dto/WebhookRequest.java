package review.pr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class WebhookRequest {

    @NotNull(message = "Payload cannot be null")
    private Map<String, Object> payload;

    @NotBlank(message = "GitHub event header cannot be blank")
    private String githubEvent;

    public WebhookRequest(Map<String, Object> payload, String githubEvent) {
        this.payload = payload;
        this.githubEvent = githubEvent;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getGithubEvent() {
        return githubEvent;
    }

    public void setGithubEvent(String githubEvent) {
        this.githubEvent = githubEvent;
    }
}
