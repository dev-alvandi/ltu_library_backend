package com.noahalvandi.dbbserver.dto.projection.film;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class FilmsReleasedDateRange {

    private Date minDate;
    private Date maxDate;
}
