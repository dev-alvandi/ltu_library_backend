package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.LanguageCount;
import com.noahalvandi.dbbserver.dto.projection.film.FilmFilterCriteria;
import com.noahalvandi.dbbserver.dto.projection.film.FilmsReleasedDateRange;
import com.noahalvandi.dbbserver.dto.response.FilmResponse;
import com.noahalvandi.dbbserver.dto.response.FilmSuggestionsResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.FilmResponseMapper;
import com.noahalvandi.dbbserver.exception.ResourceException;
import com.noahalvandi.dbbserver.model.Film;
import com.noahalvandi.dbbserver.repository.FilmCategoryRepository;
import com.noahalvandi.dbbserver.repository.FilmCopyRepository;
import com.noahalvandi.dbbserver.repository.FilmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmServiceImplementation implements FilmService {

    private final FilmRepository filmRepository;
    private final FilmCopyRepository filmCopyRepository;
    private final FilmCategoryRepository filmCategoryRepository;
    private final S3Service s3Service;

//    @Override
    public Page<FilmResponse> getAllFilms(Pageable pageable) {
        Page<Film> films = filmRepository.findAllAvailableFilmsToBorrow(pageable);
        Page<FilmResponse> pageFilmDtos = films.map(FilmResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(pageFilmDtos);

        addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(pageFilmDtos);
        return pageFilmDtos;
    }
    
//@Override
    public Map<String, Long> getAllLanguagesAndTheirCounts() {
        List<LanguageCount> counts = filmRepository.getAllLanguagesAndTheirCounts();
        return counts.stream()
                .collect(Collectors.toMap(LanguageCount::getLanguage, LanguageCount::getCount));
    }

//    @Override
    public List<String> getAllLanguages() {
        return filmRepository.getAllLanguages();
    }


//    @Override
    public FilmsReleasedDateRange getFilmsReleasedDateRange() {
        return filmRepository.getFilmsReleasedDateRange();
    }
//
//    @Override
    public Page<FilmResponse> getFilteredFilms(Pageable pageable, FilmFilterCriteria filmFilterCriteria) {
        Boolean isAvailable = filmFilterCriteria.isAvailable();
        List<String> categories = filmFilterCriteria.getCategories();
        List<String> languages = filmFilterCriteria.getLanguages();

        if (categories != null && categories.isEmpty()) categories = null;
        if (languages != null && languages.isEmpty()) languages = null;

        int ageRating = 0;

        try {
            ageRating = filmFilterCriteria.getAgeRating();
        } catch (NumberFormatException e) {
            System.out.println("Invalid integer: " + ageRating);
            throw ResourceException.forbidden("Age rating should be a number!");
        }

        Page<Film> films = filmRepository.findFilmsByFilters(
                isAvailable,
                filmFilterCriteria.getMinReleasedDate(),
                filmFilterCriteria.getMaxReleasedDate(),
                categories,
                languages,
                ageRating,
                pageable
        );

        Page<FilmResponse> filmResponses = films.map(FilmResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(filmResponses);

        return addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(filmResponses);
    }
//
//
//    @Override
    public FilmSuggestionsResponse getSuggestions(String query) {
        FilmSuggestionsResponse filmSuggestionsResponse = new FilmSuggestionsResponse();

        filmSuggestionsResponse.setTitle(filmRepository.findDistinctTitlesByQuery(query));
        filmSuggestionsResponse.setDirector(filmRepository.findDistinctDirectorsByQuery(query));
        filmSuggestionsResponse.setCountry(filmRepository.findDistinctCountriesByQuery(query));

        return filmSuggestionsResponse;
    }
//
//
//    @Override
    public Page<FilmResponse> getSearchedFilms(String query, Pageable pageable, FilmFilterCriteria filters) {
        Page<FilmResponse> filmResponses = filmRepository.searchWithFilters(
                query,
                filters.isAvailable(),
                filters.getMinReleasedDate(),
                filters.getMaxReleasedDate(),
                filters.getCategories(),
                filters.getLanguages(),
                filters.getAgeRating(),
                pageable
        ).map(FilmResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(filmResponses);

        return addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(filmResponses);
    }

    private Page<FilmResponse> addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(Page<FilmResponse> filmResponses) {
        filmResponses.getContent().forEach(filmResponse -> {
            filmResponse.setNumberOfCopies(
                    filmCopyRepository.countAllNonReferenceCopiesByFilmId(filmResponse.getFilmId())
            );
            filmResponse.setNumberOfAvailableToBorrowCopies(
                    filmCopyRepository.numberOfAvailableFilmCopiesToBorrow(filmResponse.getFilmId())
            );
        });

        return filmResponses;
    }

}
