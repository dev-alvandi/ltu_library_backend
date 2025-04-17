package com.noahalvandi.dbbserver.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BooksPublishedYearRange {

    private int minYear;
    private int maxYear;
}
