package guru.springframework.spring6restclient.client;

import guru.springframework.spring6restclient.config.OAuthClientInterceptor;
import guru.springframework.spring6restclient.dto.BeerDTO;
import guru.springframework.spring6restclient.dto.BeerDTOPageImpl;
import guru.springframework.spring6restclient.dto.BeerStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest
public class BeerClientMockTest {

    static final String URL = "http://localhost:8081";
    public static final String BEARER_TEST = "Bearer test";

    BeerClient beerClient;

    MockRestServiceServer server;

    @Autowired
    RestTemplateBuilder restTemplateBuilderConfigured;

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    RestTemplateBuilder mockRestTemplateBuilder;

    BeerDTO dto;

    String dtoJson;

    @MockitoBean
    OAuth2AuthorizedClientManager manager;

    @Autowired
    OAuthClientInterceptor oAuthClientInterceptor;

    @TestConfiguration
    public static class TestConfig {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository(
            @Value("${spring.security.oauth2.client.registration.springauth.provider}") String springAuthProviderId) {
            return new InMemoryClientRegistrationRepository(ClientRegistration
                .withRegistrationId(springAuthProviderId)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("test")
                .tokenUri("test")
                .build());
        }

        @Bean
        OAuth2AuthorizedClientService oAuth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }

        @Bean
        OAuthClientInterceptor oAuthClientInterceptor(
            OAuth2AuthorizedClientManager manager,
            ClientRegistrationRepository clientRegistrationRepository) {
            return new OAuthClientInterceptor(manager, clientRegistrationRepository);
        }

    }

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ClientRegistration clientRegistration = clientRegistrationRepository
            .findByRegistrationId("springauth");

        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
            "test", Instant.MIN, Instant.MAX);

        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(clientRegistration,
            "test", token));

        RestTemplate restTemplate = restTemplateBuilderConfigured.build();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        when(mockRestTemplateBuilder.build()).thenReturn(restTemplate);

        //NOTE: RestClient is built using the mockRestTemplateBuilder.
        //beerClient = new BeerClientImpl(RestClient.builder(mockRestTemplateBuilder.build()));
        beerClient = new BeerClientImpl(
            RestClient.builder(mockRestTemplateBuilder.build())
                .requestInterceptor(oAuthClientInterceptor)
        );
        dto = getBeerDto();
        dtoJson = objectMapper.writeValueAsString(dto);
    }

    @Test
    void testListBeersWithQueryParam() {
        String response = objectMapper.writeValueAsString(getPagePayload());

        URI uri = UriComponentsBuilder.fromUriString(BeerClientImpl.LIST_BEER_PATH)
            .queryParam("beerName", "ALE")
            .build().toUri();

        server.expect(method(HttpMethod.GET))
            .andExpect(requestTo(uri))
            .andExpect(header("Authorization", BEARER_TEST))
            .andExpect(queryParam("beerName", "ALE"))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        Page<BeerDTO> responsePage = beerClient
            .listBeers("ALE", null, null, null, null);

        assertThat(responsePage.getContent()).hasSize(1);
    }

    @Test
    void testDeleteNotFound() {
        server.expect(method(HttpMethod.DELETE))
            .andExpect(requestToUriTemplate(BeerClientImpl.DELETE_BEER_BY_ID_PATH,
                dto.getId()))
            .andExpect(header("Authorization", BEARER_TEST))
            .andRespond(withResourceNotFound());

        assertThrows(HttpClientErrorException.class, () -> beerClient.deleteBeer(dto.getId()));

        server.verify();
    }

    @Test
    void testDeleteBeer() {
        server.expect(method(HttpMethod.DELETE))
            .andExpect(requestToUriTemplate(BeerClientImpl.DELETE_BEER_BY_ID_PATH,
                dto.getId()))
            .andExpect(header("Authorization", BEARER_TEST))
            .andRespond(withNoContent());

        beerClient.deleteBeer(dto.getId());

        server.verify();
    }

    @Test
    void testUpdateBeer() {
        server.expect(method(HttpMethod.PUT))
            .andExpect(requestToUriTemplate(BeerClientImpl.UPDATE_BEER_BY_ID_PATH,
                dto.getId()))
            .andExpect(header("Authorization", BEARER_TEST))
            .andRespond(withNoContent());

        mockGetOperation();

        BeerDTO responseDto = beerClient.updateBeer(dto);
        assertThat(responseDto.getId()).isEqualTo(dto.getId());
    }

    @Test
    void testCreateBeer() {
        URI uri = UriComponentsBuilder.fromPath(BeerClientImpl.GET_BEER_BY_ID_PATH)
            .build(dto.getId());

        server.expect(method(HttpMethod.POST))
            .andExpect(requestTo(BeerClientImpl.CREATE_BEER_PATH))
            .andExpect(header("Authorization", BEARER_TEST))
            .andRespond(withAccepted().location(uri));

        mockGetOperation();

        BeerDTO responseDto = beerClient.createBeer(dto);
        assertThat(responseDto.getId()).isEqualTo(dto.getId());
    }

    @Test
    void testGetById() {

        mockGetOperation();

        BeerDTO responseDto = beerClient.getBeerById(dto.getId());
        assertThat(responseDto.getId()).isEqualTo(dto.getId());
    }

    private void mockGetOperation() {
        server.expect(method(HttpMethod.GET))
            .andExpect(requestToUriTemplate(BeerClientImpl.GET_BEER_BY_ID_PATH, dto.getId()))
            .andExpect(header("Authorization", BEARER_TEST))
            .andRespond(withSuccess(dtoJson, MediaType.APPLICATION_JSON));
    }

    @Test
    void testListBeers() {
        String payload = objectMapper.writeValueAsString(getPagePayload());

        server.expect(method(HttpMethod.GET))
            .andExpect(requestTo(BeerClientImpl.LIST_BEER_PATH))
            .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Page<BeerDTO> dtos = beerClient.listBeers();
        assertThat(dtos.getContent()).hasSizeGreaterThan(0);
    }

    private BeerDTO getBeerDto() {
        return BeerDTO.builder()
            .id(UUID.randomUUID())
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bobs")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("123245")
            .build();
    }

    private PagePayload<BeerDTO> getPagePayload() {
        return new PagePayload<>(
            singletonList(getBeerDto()),
            new BeerDTOPageImpl.PageMetadata(25, 1, 1L, 1)
        );
    }

    private record PagePayload<T>(List<T> content, BeerDTOPageImpl.PageMetadata page) {
    }
}
