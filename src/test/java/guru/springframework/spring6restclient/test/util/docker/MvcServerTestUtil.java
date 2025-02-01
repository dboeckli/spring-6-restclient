package guru.springframework.spring6restclient.test.util.docker;

import guru.springframework.spring6restclient.client.BeerClient;
import guru.springframework.spring6restclient.dto.BeerDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.data.domain.Page;

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
}
