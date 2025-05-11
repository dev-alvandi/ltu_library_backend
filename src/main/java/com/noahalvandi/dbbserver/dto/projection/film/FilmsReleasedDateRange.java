package com.noahalvandi.dbbserver.dto.projection.film;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class FilmsReleasedDateRange {

    private LocalDate minDate;
    private LocalDate maxDate;
}
