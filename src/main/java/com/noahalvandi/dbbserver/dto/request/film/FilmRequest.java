package com.noahalvandi.dbbserver.dto.request.film;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FilmRequest {

    private UUID filmId;

    private String title;
    private String director;
    private LocalDate releasedDate;
    private int ageRating;
    private String country;
    private String language;
    private String filmCategory;

    public FilmRequest(String title, 
                       String director,
                       LocalDate releasedDate,
                       int ageRating, 
                       String country, 
                       String language, 
                       String filmCategory) {
        this.title = title;
        this.director = director;
        this.releasedDate = releasedDate;
        this.ageRating = ageRating;
        this.country = country;
        this.language = language;
        this.filmCategory = filmCategory;
    }
}
