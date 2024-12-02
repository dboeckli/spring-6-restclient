package guru.springframework.spring6restclient.client;


import guru.springframework.spring6restclient.dto.BeerDTO;
import guru.springframework.spring6restclient.dto.BeerDTOPageImpl;
import guru.springframework.spring6restclient.dto.BeerStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */
@Service
@RequiredArgsConstructor
public class BeerClientImpl implements BeerClient {

    public static final String GET_BEER_PATH = "/api/v1/beer";
    public static final String GET_BEER_BY_ID_PATH = "/api/v1/beer/{beerId}";

    private final RestClient.Builder restClientBuilder;

    @Override
    public Page<BeerDTO> listBeers() {
        return listBeers(null, null, null, null, null);
    }

    @Override
    public Page<BeerDTO> listBeers(String beerName, BeerStyle beerStyle, Boolean showInventory, Integer pageNumber, Integer pageSize) {
        RestClient restClient = restClientBuilder.build();

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(GET_BEER_PATH);

        if (beerName != null) {
            uriComponentsBuilder.queryParam("beerName", beerName);
        }

        if (beerStyle != null) {
            uriComponentsBuilder.queryParam("beerStyle", beerStyle);
        }

        if (showInventory != null) {
            uriComponentsBuilder.queryParam("showInventory", beerStyle);
        }

        if (pageNumber != null) {
            uriComponentsBuilder.queryParam("pageNumber", beerStyle);
        }

        if (pageSize != null) {
            uriComponentsBuilder.queryParam("pageSize", beerStyle);
        }

        return restClient.get()
            .uri(uriComponentsBuilder.toUriString())
            .retrieve()
            .body(BeerDTOPageImpl.class);
    }

    @Override
    public BeerDTO getBeerById(UUID beerId) {
        RestClient restClient = restClientBuilder.build();

        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path(GET_BEER_BY_ID_PATH).build(beerId))
            .retrieve()
            .body(BeerDTO.class);
    }

    @Override
    public BeerDTO createBeer(BeerDTO newDto) {
        RestClient restClient = restClientBuilder.build();

        URI location = restClient.post()
            .uri(uriBuilder -> uriBuilder.path(GET_BEER_PATH).build())
            .body(newDto)
            .retrieve()
            .toBodilessEntity()
            .getHeaders()
            .getLocation();
        
        return restClient.get()
            .uri(location.getPath())
            .retrieve()
            .body(BeerDTO.class);
    }

    @Override
    public BeerDTO updateBeer(BeerDTO beerDto) {
        RestClient restClient = restClientBuilder.build();
        
        restClient.put()
            .uri(uriBuilder -> uriBuilder.path(GET_BEER_BY_ID_PATH).build(beerDto.getId()))
            .body(beerDto)
            .retrieve()
            .toBodilessEntity();
        
        return getBeerById(beerDto.getId());
    }

    @Override
    public void deleteBeer(UUID beerId) {
        RestClient restClient = restClientBuilder.build();
        
        restClient.delete()
            .uri(uriBuilder -> uriBuilder.path(GET_BEER_BY_ID_PATH).build(beerId))
            .retrieve()
            .toBodilessEntity();
    }
}
