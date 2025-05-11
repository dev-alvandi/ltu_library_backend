package com.noahalvandi.dbbserver.service.film;

import com.noahalvandi.dbbserver.dto.projection.film.FilmCategoryCount;
import com.noahalvandi.dbbserver.repository.film.FilmCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmCategoryServiceImplementation implements FilmCategoryService {

    private final FilmCategoryRepository filmCategoryRepository;

    @Override
    public Map<String, Long> getAllFilmsCategoriesAndTheirCounts() {

        List<FilmCategoryCount> counts = filmCategoryRepository.getAllCategoriesAndTheirCounts();
        return counts.stream()
                .collect(Collectors.toMap(FilmCategoryCount::getGenre,
                        FilmCategoryCount::getCount));
    }

    @Override
    public List<String> getAllFilmCategories() {
        return filmCategoryRepository.findAllFilmCategoryGenres();
    }
}
