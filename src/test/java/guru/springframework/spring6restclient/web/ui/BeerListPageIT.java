package guru.springframework.spring6restclient.web.ui;

import guru.springframework.spring6restclient.client.BeerClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static guru.springframework.spring6restclient.test.util.docker.MvcServerTestUtil.checkMvcDatabaseInitDone;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BeerListPageIT {

    @LocalServerPort
    private int port;

    private WebDriver webDriver;

    @BeforeAll
    static void setUp(@Autowired BeerClient beerClient) {
        checkMvcDatabaseInitDone(beerClient);
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // Run in headless mode
        webDriver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }

    @Test
    @Order(0)
    void testBeerListPageLoads() {
        webDriver.get("http://localhost:" + port + "/beers");
        waitForPageLoad();
        assertEquals("Beer List", webDriver.getTitle());
    }

    @Test
    @Order(1)
     void testBeerListContainsItems() {
        webDriver.get("http://localhost:" + port + "/beers");
        waitForPageLoad();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        List<WebElement> beerRows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#beerTable tbody tr")));
        
        log.info("### Found {} beer rows", beerRows.size());
        
        assertFalse(beerRows.isEmpty(), "Beer list should contain items");
        assertEquals(25, beerRows.size());
    }

    @Test
    @Order(2)
    void testPaginationExists() {
        webDriver.get("http://localhost:" + port + "/beers");
        waitForPageLoad();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        // Check if pagination exists
        WebElement pagination = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pagination")));
        assertNotNull(pagination, "Pagination should exist");

        // Check if 'Previous' and 'First' are disabled on the first page
        WebElement previousButton = webDriver.findElement(By.id("previousPage"));
        WebElement firstButton = webDriver.findElement(By.id("firstPage"));
        assertTrue(Objects.requireNonNull(previousButton.findElement(By.xpath("./..")).getAttribute("class")).contains("disabled"), "Previous button should be disabled");
        assertTrue(Objects.requireNonNull(firstButton.findElement(By.xpath("./..")).getAttribute("class")).contains("disabled"), "First button should be disabled");

        // Check if page 1 is selected
        WebElement page1Button = webDriver.findElement(By.id("page1"));
        assertTrue(Objects.requireNonNull(page1Button.findElement(By.xpath("./..")).getAttribute("class")).contains("active"), "Page 1 should be selected");

        // Check number of page number buttons (this may vary, so we'll just ensure there's at least one)
        List<WebElement> pageNumberButtons = webDriver.findElements(By.cssSelector(".pagination .page-item:not(.disabled) .page-link"));
        assertEquals(7, pageNumberButtons.size(), "There should be at least one page number button");

        // Check if 'Next' is clickable
        WebElement nextButton = webDriver.findElement(By.id("nextPage"));
        assertTrue(nextButton.isEnabled(), "Next button should be enabled");
        assertFalse(Objects.requireNonNull(nextButton.findElement(By.xpath("./..")).getAttribute("class")).contains("disabled"), "Next button should not be disabled");

        // Check if 'Last' page button exists and is not the same as the first page
        WebElement lastButton = webDriver.findElement(By.id("lastPage"));
        assertNotNull(lastButton, "Last page button should exist");
        assertEquals("21", lastButton.getText(), "Last page should not be page 1");
    }

    @Test
    @Order(3)
    void testViewButtonWorks() {
        webDriver.get("http://localhost:" + port + "/beers");
        waitForPageLoad();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        // Wait for the table and the first "View" button to be present
        WebElement firstViewButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#beerTable tbody tr:first-child .btn-primary")));
        String href = firstViewButton.getAttribute("href");

        assertNotNull(href, "View button should have a href attribute");
        assertTrue(href.contains("/beer/"), "View button should link to a specific beer");

        // Extract the beer ID from the href
        String expectedBeerId = href.substring(href.lastIndexOf("/") + 1);

        // Click the "View" button
        firstViewButton.click();

        // Wait for the new page to load
        wait.until(ExpectedConditions.titleIs("Beer Details"));

        // Find the beer ID element
        WebElement beerIdElement = webDriver.findElement(By.id("beerId"));

        assertAll("Beer Details Page Assertions",
            () -> assertEquals("http://localhost:" + port + "/beer/" + expectedBeerId, webDriver.getCurrentUrl(),
                "Should navigate to the specific beer page"),
            () -> assertEquals("Beer Details", webDriver.getTitle(),
                "Page title should be 'Beer Details'"),
            () -> assertEquals(expectedBeerId, beerIdElement.getText(),
                "Displayed beer ID should match the expected ID")
        );
   }

    private void waitForPageLoad() {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        wait.until((ExpectedCondition<Boolean>) wd ->
            Objects.equals(((JavascriptExecutor) wd).executeScript("return document.readyState"), "complete"));
    }
}
