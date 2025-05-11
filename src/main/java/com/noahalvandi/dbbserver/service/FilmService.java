package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.film.FilmFilterCriteria;
import com.noahalvandi.dbbserver.dto.projection.film.FilmsReleasedDateRange;
import com.noahalvandi.dbbserver.dto.request.film.FilmCopyRequest;
import com.noahalvandi.dbbserver.dto.request.film.FilmRequest;
import com.noahalvandi.dbbserver.dto.response.film.FilmCopyResponse;
import com.noahalvandi.dbbserver.dto.response.film.FilmResponse;
import com.noahalvandi.dbbserver.dto.response.film.FilmSuggestionsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FilmService {
    Page<FilmResponse> getAllFilms(Pageable pageable);

    Map<String, Long> getAllLanguagesAndTheirCounts();

    List<String> getAllLanguages();

    FilmsReleasedDateRange getFilmsReleasedDateRange();

    Page<FilmResponse> getFilteredFilms(Pageable pageable, FilmFilterCriteria filmFilterCriteria);

    FilmSuggestionsResponse getSuggestions(String query);

    Page<FilmResponse> getSearchedFilms(String query, Pageable pageable, FilmFilterCriteria filters);

    FilmResponse createFilm(FilmRequest filmRequest, MultipartFile image) throws Exception;

    FilmResponse updateFilm(FilmRequest filmRequest, MultipartFile image) throws IOException;

    FilmResponse getRequestedFilm(UUID filmId);

    Page<FilmCopyResponse> getFilmCopiesByFilmId(UUID filmId, Pageable pageable);

    FilmCopyResponse createFilmCopy(UUID filmId, FilmCopyRequest request) throws Exception;

    void deleteFilmAndCopies(UUID filmId);

    void deleteFilmCopyByFilmCopyId(UUID filmCopyId);

    FilmCopyResponse updateFilmCopy(UUID filmCopyId, FilmCopyRequest request) throws Exception;
}
