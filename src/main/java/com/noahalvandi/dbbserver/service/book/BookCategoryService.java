package com.noahalvandi.dbbserver.service.book;

import java.util.List;
import java.util.Map;

public interface BookCategoryService {

    public Map<String, Long> getAllBooksCategoriesAndTheirCounts();

    public List<String> getAllBookCategories();

}
