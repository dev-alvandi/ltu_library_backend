package com.noahalvandi.dbbserver.dto.projection.film;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@ToString
public class FilmFilterCriteria {

    private boolean isAvailable;
    private Date minReleasedDate;
    private Date maxReleasedDate;
    private List<String> categories;
    private List<String> languages;
    private int ageRating;
}
