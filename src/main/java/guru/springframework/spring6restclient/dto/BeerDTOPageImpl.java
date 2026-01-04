package guru.springframework.spring6restclient.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true, value = "pageable")
public class BeerDTOPageImpl extends PageImpl<BeerDTO> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public BeerDTOPageImpl(
        @JsonProperty("content")
        List<guru.springframework.spring6restclient.dto.BeerDTO> content,

        @JsonProperty("page")
        PagedModel.PageMetadata page
    ) {
        super(
            content,
            PageRequest.of(Math.toIntExact(page.number()), Math.toIntExact(page.size())),
            page.totalElements()
        );
    }

    public BeerDTOPageImpl(
        List<guru.springframework.spring6restclient.dto.BeerDTO> content,
        Pageable pageable,
        long total
    ) {
        super(content, pageable, total);
    }

    public BeerDTOPageImpl(List<guru.springframework.spring6restclient.dto.BeerDTO> content) {
        super(content);
    }
}

