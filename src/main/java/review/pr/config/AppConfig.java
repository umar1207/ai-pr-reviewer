package review.pr.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableRetry
@Slf4j
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    @Bean
    public AppProperties appProperties() {
        return new AppProperties();
    }
}

@ConfigurationProperties(prefix = "app")
class AppProperties {
    private Validation validation = new Validation();

    public Validation getValidation() {
        return validation;
    }

    public void setValidation(Validation validation) {
        this.validation = validation;
    }

    public static class Validation {
        private boolean enabled = true;
        private String githubEventHeader = "x-github-event";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getGithubEventHeader() {
            return githubEventHeader;
        }

        public void setGithubEventHeader(String githubEventHeader) {
            this.githubEventHeader = githubEventHeader;
        }
    }
}
