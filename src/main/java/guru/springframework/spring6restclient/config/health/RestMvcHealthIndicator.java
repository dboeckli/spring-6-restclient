package guru.springframework.spring6restclient.config.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class RestMvcHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    private final String restMvcUrl;

    private boolean wasDownLastCheck = true;

    public RestMvcHealthIndicator(@Value("${rest.mvcUrl}") String restMvcUrl) {
        this.restTemplate = new RestTemplateBuilder().build();
        this.restMvcUrl = restMvcUrl;
    }

    @Override
    public Health health() {
        try {
            String response = restTemplate.getForObject(restMvcUrl + "/actuator/health", String.class);
            if (response != null && response.contains("\"status\":\"UP\"")) {
                if (wasDownLastCheck) {
                    log.info("MVC server is ready again under {}", restMvcUrl);
                }
                wasDownLastCheck = false;
                return Health.up().build();
            } else {
                log.warn("MVC server is not reporting UP status at {}", restMvcUrl);
                wasDownLastCheck = true;
                return Health.down().build();
            }
        } catch (Exception e) {
            log.warn("MVC server is not reachable at {}", restMvcUrl, e);
            wasDownLastCheck = true;
            return Health.down(e).build();
        }
    }

}
