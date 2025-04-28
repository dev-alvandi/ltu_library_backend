package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.model.BookCategory;

import java.util.List;
import java.util.Map;

public interface BookCategoryService {

    public Map<String, Long> getAllBooksCategoriesAndTheirCounts();

    public List<String> getAllBookCategories();

}
