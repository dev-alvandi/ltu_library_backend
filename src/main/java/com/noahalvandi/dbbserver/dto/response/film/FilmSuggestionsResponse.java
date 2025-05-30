package com.noahalvandi.dbbserver.dto.response.film;

import lombok.Data;

import java.util.List;

@Data
public class FilmSuggestionsResponse {
    private List<String> title;
    private List<String> director;
    private List<String> country;
}
