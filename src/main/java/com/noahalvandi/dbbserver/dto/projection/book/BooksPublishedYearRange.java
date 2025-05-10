package com.noahalvandi.dbbserver.dto.projection.book;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BooksPublishedYearRange {

    private Integer minYear;
    private Integer maxYear;
}
