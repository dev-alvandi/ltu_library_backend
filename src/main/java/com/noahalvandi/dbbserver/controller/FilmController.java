package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.dto.projection.LanguagesCategories;
import com.noahalvandi.dbbserver.dto.projection.film.FilmFilterCriteria;
import com.noahalvandi.dbbserver.dto.projection.film.FilmsReleasedDateRange;
import com.noahalvandi.dbbserver.dto.request.film.FilmCopyRequest;
import com.noahalvandi.dbbserver.dto.request.film.FilmRequest;
import com.noahalvandi.dbbserver.dto.response.film.FilmCopyResponse;
import com.noahalvandi.dbbserver.dto.response.film.FilmResponse;
import com.noahalvandi.dbbserver.dto.response.film.FilmSuggestionsResponse;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.service.UserService;
import com.noahalvandi.dbbserver.service.film.FilmCategoryService;
import com.noahalvandi.dbbserver.service.film.FilmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;
    private final FilmCategoryService filmCategoryService;
    private final UserService userService;


    @GetMapping("/films")
    public ResponseEntity<Page<FilmResponse>> allFilms(@PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<FilmResponse> films = filmService.getAllFilms(pageable);
        return new ResponseEntity<>(films, HttpStatus.OK);
    }

    @GetMapping("/films-categories-and-counts")
    public ResponseEntity<Map<String, Long>> allFilmsCategoriesAndCounts() {

        Map<String, Long> filmCategories = filmCategoryService.getAllFilmsCategoriesAndTheirCounts();

        return new ResponseEntity<>(filmCategories, HttpStatus.OK);
    }

    @GetMapping("/films-categories-and-languages")
    public ResponseEntity<LanguagesCategories> allFilmsCategoriesAndLanguages() {

        List<String> filmCategories = filmCategoryService.getAllFilmCategories();
        List<String> filmLanguages = filmService.getAllLanguages();

        LanguagesCategories blc = new LanguagesCategories();

        blc.setCategories(filmCategories);
        blc.setLanguages(filmLanguages);

        return new ResponseEntity<>(blc, HttpStatus.OK);
    }

    @GetMapping("/films-categories")
    public ResponseEntity<List<String>> allFilmsCategories(@RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        List<String> filmCategories = filmCategoryService.getAllFilmCategories();

        return new ResponseEntity<>(filmCategories, HttpStatus.OK);
    }

    @GetMapping("/films-languages")
    public ResponseEntity<List<String>> allFilmsLanguages(@RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        List<String> filmCategories = filmService.getAllLanguages();

        return new ResponseEntity<>(filmCategories, HttpStatus.OK);
    }

    @GetMapping("/films-languages-and-counts")
    public ResponseEntity<Map<String, Long>> allFilmsLanguagesAndCounts() {

        Map<String, Long> languagesCounts = filmService.getAllLanguagesAndTheirCounts();

        return new ResponseEntity<>(languagesCounts, HttpStatus.OK);
    }

    @GetMapping("/films-year-range")
    public ResponseEntity<FilmsReleasedDateRange> allFilmsYearRange() {

        FilmsReleasedDateRange filmsPublishedYearRange = filmService.getFilmsReleasedDateRange();

        return new ResponseEntity<>(filmsPublishedYearRange, HttpStatus.OK);
    }

    @GetMapping("/films-filters")
    public ResponseEntity<?> allFilmsFilters() {

        FilmsReleasedDateRange yeaRange = filmService.getFilmsReleasedDateRange();
        Map<String, Long> categories = filmCategoryService.getAllFilmsCategoriesAndTheirCounts();
        Map<String, Long> languages = filmService.getAllLanguagesAndTheirCounts();

        Map<String, Object> response = new HashMap<>();
        response.put("publishedYearRange", yeaRange);
        response.put("categories", categories);
        response.put("languages", languages);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/filtered-films")
    public ResponseEntity<Page<FilmResponse>> getFilteredFilms(
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Date minReleasedDate,
            @RequestParam(required = false) Date maxReleasedDate,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> languages,
            @RequestParam(required = false) Integer ageRating,
            @PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {

        FilmFilterCriteria filter = new FilmFilterCriteria(
                isAvailable, minReleasedDate, maxReleasedDate, categories, languages, ageRating
        );

        Page<FilmResponse> filteredFilms = filmService.getFilteredFilms(pageable, filter);
        return new ResponseEntity<>(filteredFilms, HttpStatus.OK);
    }

    @GetMapping("/suggested-films")
    public ResponseEntity<FilmSuggestionsResponse> getSuggestions(@RequestParam String query) {

        FilmSuggestionsResponse response = filmService.getSuggestions(query);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/searched-films")
    public ResponseEntity<Page<FilmResponse>> getSearchedFilm(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Date minReleasedDate,
            @RequestParam(required = false) Date maxReleasedDate,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> languages,
            @RequestParam(required = false) Integer ageRating,
            @PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {

        FilmFilterCriteria filters = new FilmFilterCriteria(
                isAvailable, minReleasedDate, maxReleasedDate, categories, languages, ageRating);

        Page<FilmResponse> result = filmService.getSearchedFilms(query, pageable, filters);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/film/{filmId}")
    public ResponseEntity<FilmResponse> getRequestedFilm(
            @PathVariable UUID filmId,
            @RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        FilmResponse fetchedFilm = filmService.getRequestedFilm(filmId);

        if (fetchedFilm == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(fetchedFilm);
    }

    @DeleteMapping("/film/{filmId}")
    public ResponseEntity<String> deleteFilm(
            @PathVariable UUID filmId,
            @RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        filmService.deleteFilmAndCopies(filmId);

        return new ResponseEntity<>("Film deleted", HttpStatus.NO_CONTENT);
    }

    @GetMapping("/film/{filmId}/film-copies")
    public ResponseEntity<Page<FilmCopyResponse>> getFilmCopies(
                @PathVariable UUID filmId,
                @RequestHeader("Authorization") String jwt,
                @PageableDefault(page = 0, size = 5) Pageable pageable) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<FilmCopyResponse> filmCopies = filmService.getFilmCopiesByFilmId(filmId, pageable);

        return new ResponseEntity<>(filmCopies, HttpStatus.OK);
    }

    @PostMapping("/film-copy/{filmId}/create")
    public ResponseEntity<FilmCopyResponse> createFilmCopy(
            @PathVariable UUID filmId,
            @RequestBody FilmCopyRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FilmCopyResponse createdCopy = filmService.createFilmCopy(filmId, request);


        return new ResponseEntity<>(createdCopy, HttpStatus.CREATED);
    }

    @DeleteMapping("/film-copy/{filmCopyId}")
    public ResponseEntity<String> deleteFilmCopy(
            @PathVariable UUID filmCopyId,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        filmService.deleteFilmCopyByFilmCopyId(filmCopyId);

        return new ResponseEntity<>("Film Copy deleted", HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/film/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FilmResponse> createFilm(
            @RequestPart("title") String title,
            @RequestPart("director") String director,
            @RequestPart("releasedDate") LocalDate releasedDate,
            @RequestPart("ageRating") Integer ageRating,
            @RequestPart("country") String country,
            @RequestPart("language") String language,
            @RequestPart("category") String category,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);
        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FilmRequest filmRequest = new FilmRequest(title, director, releasedDate, ageRating,
                country, language, category);

        FilmResponse created = filmService.createFilm(filmRequest, image);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    @PutMapping(value = "/film/{filmId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FilmResponse> updateFilm(
            @PathVariable UUID filmId,
            @RequestPart("title") String title,
            @RequestPart("director") String director,
            @RequestPart("releasedDate") LocalDate releasedDate,
            @RequestPart("ageRating") Integer ageRating,
            @RequestPart("country") String country,
            @RequestPart("language") String language,
            @RequestPart("category") String category,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String jwt) throws UserException, IOException {

        User user = userService.findUserProfileByJwt(jwt);
        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FilmRequest filmRequest = new FilmRequest(title, director, releasedDate, ageRating,
                country, language, category);


        FilmResponse created = filmService.updateFilm(filmRequest, image);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping(value = "/film-copy/{filmCopyId}/update")
    public ResponseEntity<FilmCopyResponse> updateFilmCopy(
            @PathVariable UUID filmCopyId,
            @RequestBody FilmCopyRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FilmCopyResponse filmCopyResponse = filmService.updateFilmCopy(filmCopyId, request);

        return new ResponseEntity<>(filmCopyResponse, HttpStatus.CREATED);
    }
}
