package review.pr.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import review.pr.exception.general.DiffFetchException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DiffService {

    private final RestTemplate restTemplate;

    @Value("${github-access-token}")
    private String githubAccessToken;

    public DiffService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
        value = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String fetchDiff(String diffUrl) {
        log.info("Fetching diff from: {}", maskUrl(diffUrl));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + githubAccessToken);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            String diffContent = restTemplate.exchange(diffUrl, HttpMethod.GET, request, String.class).getBody();

            log.info("Successfully fetched diff content (length: {})", diffContent != null ? diffContent.length() : 0);
            return diffContent;
        } catch (RestClientException e) {
            log.error("Failed to fetch diff from {}", maskUrl(diffUrl), e);
            throw new DiffFetchException("Failed to fetch diff: " + e.getMessage());
        }
    }

    public Map<String, Integer> computeCommentPositions(String rawDiff) {
        log.debug("Computing comment positions from diff");
        Map<String, Integer> fileToPosition = new LinkedHashMap<>();

        if (rawDiff == null || rawDiff.isEmpty()) {
            log.warn("Empty diff content provided");
            return fileToPosition;
        }

        List<String> diffLines = rawDiff.lines().toList();
        String currentFile = null;
        boolean inHunk = false;
        int position = 0;
        boolean found = false;

        for (String line : diffLines) {
            // New file diff starts
            if (line.startsWith("diff --git")) {
                inHunk = false;
                found = false;
                position = 0;
                currentFile = null;
            }

            // Extract filename from '+++ b/filename'
            if (line.startsWith("+++ b/")) {
                currentFile = line.substring(6);
            }

            // Start of a hunk
            if (line.startsWith("@@") && !found) {
                inHunk = true;
                position = 0;
                continue;
            }

            // Inside hunk: count lines until first '+' or '-' to find position
            if (inHunk && !found) {
                position++;

                if (line.startsWith("+") || line.startsWith("-")) {
                    if (currentFile != null) {
                        fileToPosition.put(currentFile, position);
                        log.debug("Position for file {}: {}", currentFile, position);
                    }
                    found = true;
                    inHunk = false;
                }
            }
        }

        log.info("Computed positions for {} files", fileToPosition.size());
        return fileToPosition;
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("api\\.github\\.com/repos/[^/]+/[^/]+", "api.github.com/repos/***/***");
    }
}
