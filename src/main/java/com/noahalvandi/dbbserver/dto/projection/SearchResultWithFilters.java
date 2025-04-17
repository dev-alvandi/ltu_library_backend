package com.noahalvandi.dbbserver.dto.projection;

import com.noahalvandi.dbbserver.dto.response.BookResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.Map;

@Data
@AllArgsConstructor
public class SearchResultWithFilters {
    private Page<BookResponse> books;
    private Map<String, Long> categories;
    private Map<String, Long> languages;
    private int minYear;
    private int maxYear;
}
