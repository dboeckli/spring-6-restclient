package guru.springframework.spring6restclient.test.util.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6restclient.client.BeerClient;
import guru.springframework.spring6restclient.dto.BeerDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;

@Slf4j
@UtilityClass
public class MvcServerTestUtil {

    public static void checkMvcDatabaseInitDone(BeerClient beerClient) {
        // Wait for the database to be fully initialized
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                try {
                    Page<BeerDTO> page = beerClient.listBeers(null, null, null, null, null);
                    log.info("### Waiting for database to be fully initialized. Inserted: Beers: {}", page.getTotalElements());
                    return page.getTotalElements() >= 2413;
                } catch (Exception e) {
                    return false;
                }
            });
        log.info("Database is fully initialized.");
    }

    public static void checkMvcReady(TestRestTemplate restTemplate, ObjectMapper objectMapper, String mvcUrl) {
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                        mvcUrl + "/actuator/health/readiness",
                        String.class
                    );
                    log.info("MVC Readiness check response: {}", response.getBody());

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode jsonNode = objectMapper.readTree(response.getBody());
                        String status = jsonNode.path("status").asText();
                        return "UP".equals(status);
                    }
                    return false;
                } catch (Exception e) {
                    log.warn("Error checking MVC readiness: ", e);
                    return false;
                }
            });
        log.info("MVC application is ready.");
    }
    
}
