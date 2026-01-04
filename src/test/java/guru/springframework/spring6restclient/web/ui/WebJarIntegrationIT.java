package guru.springframework.spring6restclient.web.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@AutoConfigureMockMvc
class WebJarIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldResolveBootstrapCssWithoutVersion() throws Exception {
        // Dieser Test simuliert einen Zugriff auf die CSS Datei OHNE Versionsnummer.
        // Dies stellt sicher, dass 'webjars-locator-lite' korrekt konfiguriert ist und funktioniert.
        mockMvc.perform(get("/webjars/bootstrap/css/bootstrap.min.css"))
            .andExpect(status().isOk()) // Erwartet HTTP 200
            .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/css"))); // Prüft ob auch wirklich CSS zurückkommt
    }

    @Test
    void shouldResolveBootstrapJsWithoutVersion() throws Exception {
        // Optional: Test auch für JS
        mockMvc.perform(get("/webjars/bootstrap/js/bootstrap.min.js"))
            .andExpect(status().isOk());
    }
}
