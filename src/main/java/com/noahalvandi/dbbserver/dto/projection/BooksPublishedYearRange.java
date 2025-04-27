package com.noahalvandi.dbbserver.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BooksPublishedYearRange {

    private Integer minYear;
    private Integer maxYear;
}
