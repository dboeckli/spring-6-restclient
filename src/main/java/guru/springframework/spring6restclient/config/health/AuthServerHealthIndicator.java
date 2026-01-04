package guru.springframework.spring6restclient.config.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class AuthServerHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    private final String authServerUrl;

    private boolean wasDownLastCheck = true;

    public AuthServerHealthIndicator(@Value("${security.auth-server-health-url}") String authServerUrl) {
        this.restTemplate = new RestTemplateBuilder().build();
        this.authServerUrl = authServerUrl;
    }

    @Override
    public Health health() {
        try {
            String response = restTemplate.getForObject(authServerUrl + "/actuator/health", String.class);
            if (response != null && response.contains("\"status\":\"UP\"")) {
                if (wasDownLastCheck) {
                    log.info("Auth server is ready again under {}", authServerUrl);
                }
                wasDownLastCheck = false;
                return Health.up().build();
            } else {
                log.warn("Auth server is not reporting UP status at {}", authServerUrl);
                wasDownLastCheck = true;
                return Health.down().build();
            }
        } catch (Exception e) {
            log.warn("Auth server is not reachable at {}", authServerUrl, e);
            wasDownLastCheck = true;
            return Health.down(e).build();
        }
    }

}
