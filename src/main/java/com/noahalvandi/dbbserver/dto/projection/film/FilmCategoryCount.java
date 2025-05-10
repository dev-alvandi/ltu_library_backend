package com.noahalvandi.dbbserver.dto.projection.film;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilmCategoryCount {

    private String genre;
    private long count;
}
