package com.noahalvandi.dbbserver.service.film;

import java.util.List;
import java.util.Map;

public interface FilmCategoryService {

    public Map<String, Long> getAllFilmsCategoriesAndTheirCounts();

    public List<String> getAllFilmCategories();

}
