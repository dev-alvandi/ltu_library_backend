package com.noahalvandi.dbbserver.dto.request.mapper;

import com.noahalvandi.dbbserver.dto.request.film.FilmRequest;
import com.noahalvandi.dbbserver.model.Film;

public class FilmRequestMapper {
    public static Film toEntity(FilmRequest filmRequest) {

        Film film = new Film();

        film.setTitle(filmRequest.getTitle());
        film.setDirector(filmRequest.getDirector());
        film.setReleasedDate(filmRequest.getReleasedDate());
        film.setAgeRating(filmRequest.getAgeRating());
        film.setCountry(filmRequest.getCountry());
        film.setLanguage(filmRequest.getLanguage());

        return film;
        
    }
}
