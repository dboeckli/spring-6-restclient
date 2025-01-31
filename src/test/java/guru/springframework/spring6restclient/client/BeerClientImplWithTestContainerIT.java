package guru.springframework.spring6restclient.client;

import guru.springframework.spring6restclient.dto.BeerDTO;
import guru.springframework.spring6restclient.dto.BeerStyle;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("testcontainer")
@Slf4j
class BeerClientImplWithTestContainerIT {

    static final int REST_MVC_PORT = TestSocketUtils.findAvailableTcpPort();
    static final int AUTH_SERVER_PORT = TestSocketUtils.findAvailableTcpPort();
    static final int REST_GATEWAY_PORT = TestSocketUtils.findAvailableTcpPort();

    static final Network sharedNetwork = Network.newNetwork();

    @Autowired
    BeerClient beerClient;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.4")
        .withNetworkAliases("mysql")
        .withNetwork(sharedNetwork)
        .withEnv("MYSQL_DATABASE", "restdb")
        .withEnv("MYSQL_USER", "restadmin")
        .withEnv("MYSQL_PASSWORD", "password")
        .withEnv("MYSQL_ROOT_PASSWORD", "password")

        .withDatabaseName("restdb")
        .withUsername("restadmin")
        .withPassword("password")

        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("mysql")))
        .waitingFor(Wait.forSuccessfulCommand("mysqladmin ping -h localhost -uroot -ppassword"));

    @Container
    static GenericContainer<?> authServer = new GenericContainer<>("domboeckli/spring-6-auth-server:0.0.1-SNAPSHOT")
        .withNetworkAliases("auth-server")
        .withNetwork(sharedNetwork)
        .withEnv("SERVER_PORT", String.valueOf(AUTH_SERVER_PORT))
        .withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATION_SERVER_ISSUER", "http://auth-server:" + AUTH_SERVER_PORT)
        .withExposedPorts(AUTH_SERVER_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("auth-server")))
        .waitingFor(Wait.forHttp("/actuator/health/readiness")
            .forStatusCode(200)
            .forResponsePredicate(response ->
                response.contains("\"status\":\"UP\"")
            )
        );

    @Container
    static GenericContainer<?> restMvc = new GenericContainer<>("domboeckli/spring-6-rest-mvc:0.0.1")
        .withNetworkAliases("rest-mvc")
        .withExposedPorts(REST_MVC_PORT)
        .withNetwork(sharedNetwork)
        .withEnv("SPRING_PROFILES_ACTIVE", "localmysql")
        .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://auth-server:" + AUTH_SERVER_PORT)
        .withEnv("SERVER_PORT", String.valueOf(REST_MVC_PORT))
        .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql:3306/restdb")
        .withEnv("LOGGING_LEVEL_ORG_APACHE_KAFKA_CLIENTS_NETWORKCLIENT", "ERROR")
        .dependsOn(mysql, authServer)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("rest-mvc")))
        .waitingFor(Wait.forHttp("/actuator/health/readiness")
            .forStatusCode(200)
            .forResponsePredicate(response ->
                response.contains("\"status\":\"UP\"")
            )
        );

    @Container
    static GenericContainer<?> restGateway = new GenericContainer<>("domboeckli/spring-6-gateway:0.0.1-SNAPSHOT")
        .withExposedPorts(REST_GATEWAY_PORT)
        .withNetwork(sharedNetwork)
        .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://auth-server:" + AUTH_SERVER_PORT)
        .withEnv("SERVER_PORT", String.valueOf(REST_GATEWAY_PORT))
        .withEnv("SPRING_PROFILES_ACTIVE", "docker")

        // Route for spring-6-rest-mvc
        .withEnv("SPRING_CLOUD_GATEWAY_ROUTES[0]_ID", "mvc_route")
        .withEnv("SPRING_CLOUD_GATEWAY_ROUTES[0]_URI", "http://rest-mvc:" + REST_MVC_PORT)
        .withEnv("SPRING_CLOUD_GATEWAY_ROUTES[0]_PREDICATES[0]", "Path=/api/v1/**")

        // Route for spring-6-auth-server
        .withEnv("SPRING_CLOUD_GATEWAY_ROUTES[1]_ID", "auth_route")
        .withEnv("SPRING_CLOUD_GATEWAY_ROUTES[1]_URI", "http://auth-server:" + AUTH_SERVER_PORT)
        .withEnv("SPRING_CLOUD_GATEWAY_ROUTES[1]_PREDICATES[0]", "Path=/oauth2/**")

        .withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY", "INFO") // SET TRACE for detailed logs
        .withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_HTTP_SERVER_REACTIVE", "INFO") // SET DEBUG for detailed logs  
        .withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB_REACTIVE", "INFO") // SET DEBUG for detailed logs
        .withEnv("LOGGING_LEVEL_REACTOR_IPC_NETTY", "INFO") // SET DEBUG for detailed logs
        .withEnv("LOGGING_LEVEL_REACTOR_NETTY", "INFO") // SET DEBUG for detailed logs
        .withEnv("LOGGING_LEVEL_ORG_ZALANDO_LOGBOOK", "TRACE") // SET TRACE for detailed logs

        .dependsOn(authServer, restMvc)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("gateway")))
        .waitingFor(Wait.forHttp("/actuator/health/readiness")
            .forStatusCode(200)
            .forResponsePredicate(response ->
                response.contains("\"status\":\"UP\"")
            )
        );

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String mvcServerUrl = "http://" + restMvc.getHost() + ":" + restMvc.getFirstMappedPort();
        log.info("### Rest MVC Server URL: " + mvcServerUrl);
        registry.add("rest.template.rootUrl", () -> mvcServerUrl);

        String gatewayServerUrl = "http://" + restGateway.getHost() + ":" + restGateway.getFirstMappedPort();
        log.info("### Rest Gateway Server URL: " + gatewayServerUrl);
        registry.add("rest.template.gatewayUrl", () -> gatewayServerUrl);

        String authServerAuthorizationUrl = "http://" + authServer.getHost() + ":" + authServer.getFirstMappedPort() + "/auth2/authorize";
        log.info("### AuthServer Authorization Url: " + authServerAuthorizationUrl);
        registry.add("spring.security.oauth2.client.provider.springauth.authorization-uri", () -> authServerAuthorizationUrl);

        String authServerTokenUrl = "http://" + authServer.getHost() + ":" + authServer.getFirstMappedPort() + "/oauth2/token";
        log.info("### Auth Server Token Url: " + authServerTokenUrl);
        registry.add("spring.security.oauth2.client.provider.springauth.token-uri", () -> authServerTokenUrl);

        String issuerUrl = "http://auth-server:" + AUTH_SERVER_PORT;
        log.info("### Issuer Url: " + issuerUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUrl);
    }

    @BeforeAll
    static void setUp() {
        log.info("#### auth server listening on port {} and host: {} and port {}", AUTH_SERVER_PORT, authServer.getHost(), authServer.getFirstMappedPort());
        log.info("#### gateway server  listening on port {} and host: {} and port {}", REST_GATEWAY_PORT, restGateway.getHost(), restGateway.getFirstMappedPort());
        log.info("#### mvc server listening on port {} and host: {} and port {}", REST_MVC_PORT, restMvc.getHost(), restMvc.getFirstMappedPort());
    }

    @Test
    @Order(99)
    void testDeleteBeer() {
        BeerDTO newDto = BeerDTO.builder()
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bobs 2")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("123245")
            .build();

        BeerDTO beerDto = beerClient.createBeer(newDto);

        beerClient.deleteBeer(beerDto.getId());

        assertThrows(HttpClientErrorException.class, () -> {
            //should error
            beerClient.getBeerById(beerDto.getId());
        });
    }

    @Test
    @Order(51)
    void testUpdateBeer() {

        BeerDTO newDto = BeerDTO.builder()
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bobs 2")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("123245")
            .build();

        BeerDTO beerDto = beerClient.createBeer(newDto);

        final String newName = "Mango Bobs 3";
        beerDto.setBeerName(newName);
        BeerDTO updatedBeer = beerClient.updateBeer(beerDto);

        assertEquals(newName, updatedBeer.getBeerName());
    }

    @Test
    @Order(41)
    void testCreateBeer() {

        BeerDTO newDto = BeerDTO.builder()
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bobs")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("123245")
            .build();

        BeerDTO savedDto = beerClient.createBeer(newDto);
        assertNotNull(savedDto);
    }

    @Test
    @Order(3)
    void getBeerById() {
        Page<BeerDTO> beerDTOS = beerClient.listBeers();
        BeerDTO dto = beerDTOS.getContent().getFirst();
        BeerDTO byId = beerClient.getBeerById(dto.getId());
        assertEquals(dto.getId(), byId.getId());
    }

    @Test
    @Order(2)
    void listBeersNoBeerName() {
        Page<BeerDTO> beersPage = beerClient.listBeers(null, null, null, null, null);
        assertEquals(2413, beersPage.getTotalElements());
    }

    @Test
    @Order(1)
    void listBeers() {
        Page<BeerDTO> beersPage = beerClient.listBeers("IPA", null, null, null, null);
        assertEquals(336, beersPage.getTotalElements());
    }
}
