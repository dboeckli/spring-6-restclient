package guru.springframework.spring6restclient.web;

import guru.springframework.spring6restclient.client.BeerClient;
import guru.springframework.spring6restclient.dto.BeerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class BeerWebControllerTest {

    @Mock
    BeerClient beerClient;

    @Mock
    Model model;

    BeerWebController controller;

    @BeforeEach
    void setUp() {
        openMocks(this);
        controller = new BeerWebController(beerClient);
    }

    @Test
    void testGetBeers() {
        // Given
        Page<BeerDTO> beerPage = new PageImpl<>(Arrays.asList(BeerDTO.builder().build(), BeerDTO.builder().build()));
        when(beerClient.listBeers(isNull(), isNull(), isNull(), anyInt(), anyInt())).thenReturn(beerPage);

        // When
        String viewName = controller.getBeers(0, 25, model);

        // Then
        assertEquals("beers", viewName);
        verify(model).addAttribute(eq("beers"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", 1);
        verify(model).addAttribute("totalElements", 2L);
    }

    @Test
    void testGetBeerById() {
        // Given
        UUID beerId = UUID.randomUUID();
        BeerDTO beerDTO = BeerDTO.builder().build();
        when(beerClient.getBeerById(beerId)).thenReturn(beerDTO);

        // When
        String viewName = controller.getBeerById(beerId.toString(), model);

        // Then
        assertEquals("beer", viewName);
        verify(model).addAttribute("beer", beerDTO);
    }

    @Test
    void testGetBeersWithPagination() {
        int givenPageNumer = 2;
        int givenPageSize = 3;
        long givenTotalElements = 10L;
        
        // Given
        Page<BeerDTO> beerPage = new PageImpl<>(Arrays.asList(
            BeerDTO.builder().build(), 
            BeerDTO.builder().build(), 
            BeerDTO.builder().build(),
            BeerDTO.builder().build(),
            BeerDTO.builder().build(),
            BeerDTO.builder().build(),
            BeerDTO.builder().build(),
            BeerDTO.builder().build(),
            BeerDTO.builder().build(),
            BeerDTO.builder().build()
        ), Pageable.ofSize(givenPageSize).withPage(givenPageNumer), givenTotalElements);

        when(beerClient.listBeers(isNull(), isNull(), isNull(), eq(givenPageNumer), eq(givenPageSize))).thenReturn(beerPage);

        // When
        String viewName = controller.getBeers(givenPageNumer, givenPageSize, model);

        // Then
        assertEquals("beers", viewName);
        verify(model).addAttribute(eq("beers"), anyList());
        verify(model).addAttribute("currentPage", 2);
        verify(model).addAttribute("totalPages", 4);
        verify(model).addAttribute("totalElements", 10L);
    }

}
