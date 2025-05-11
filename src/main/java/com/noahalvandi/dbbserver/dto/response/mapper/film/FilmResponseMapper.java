package com.noahalvandi.dbbserver.dto.response.mapper.film;

import com.noahalvandi.dbbserver.dto.response.film.FilmResponse;
import com.noahalvandi.dbbserver.model.Film;

public class FilmResponseMapper {
    public static FilmResponse toDto(Film film) {
        FilmResponse dto = new FilmResponse();

        dto.setFilmId(film.getFilmId());
        dto.setTitle(film.getTitle());
        dto.setDirector(film.getDirector());
        dto.setReleasedDate(film.getReleasedDate());
        dto.setAgeRating(film.getAgeRating());
        dto.setCountry(film.getCountry());
        dto.setLanguage(film.getLanguage());
        dto.setImageUrl(film.getImageUrl());
        dto.setFilmCategory(film.getFilmCategory());

        return dto;
    }
}
