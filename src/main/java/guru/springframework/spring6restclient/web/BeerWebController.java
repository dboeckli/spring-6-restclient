package guru.springframework.spring6restclient.web;

import guru.springframework.spring6restclient.client.BeerClient;
import guru.springframework.spring6restclient.dto.BeerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class BeerWebController {

    private final BeerClient beerClient;

    @GetMapping("/beers")
    public String getBeers(@RequestParam(required = false) Integer pageNumber,
                           @RequestParam(required = false) Integer pageSize,
                           Model model) {
        
        pageNumber = pageNumber == null ? 0 : pageNumber;
        pageSize = pageSize == null ? 25 : pageSize;

        Page<BeerDTO> beerPage = beerClient.listBeers(null, null, null, pageNumber, pageSize);
        model.addAttribute("beers", beerPage.getContent());
        model.addAttribute("currentPage", beerPage.getNumber());
        model.addAttribute("totalPages", beerPage.getTotalPages());
        model.addAttribute("totalElements", beerPage.getTotalElements());

        int totalPages = beerPage.getTotalPages();
        if (totalPages > 0) {
            int start = Math.max(0, Math.min(pageNumber - 2, totalPages - 5));
            int end = Math.min(totalPages - 1, start + 4);
            model.addAttribute("startPage", start);
            model.addAttribute("endPage", end);
        }

        return "beers";
    }

    @GetMapping("/beer/{id}")
    public String getBeerById(@PathVariable("id") String beerId, Model model) {
        model.addAttribute("beer", beerClient.getBeerById(UUID.fromString(beerId)));
        return "beer";
    }
}
