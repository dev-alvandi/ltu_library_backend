package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.BookCategoryCount;
import com.noahalvandi.dbbserver.model.BookCategory;
import com.noahalvandi.dbbserver.repository.BookCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookCategoryServiceImplementation implements BookCategoryService {

    private final BookCategoryRepository bookCategoryRepository;

    public BookCategoryServiceImplementation(BookCategoryRepository bookCategoryRepository) {
        this.bookCategoryRepository = bookCategoryRepository;
    }

    @Override
    public Map<String, Long> getAllBooksCategoriesAndTheirCounts() {

        List<BookCategoryCount> counts = bookCategoryRepository.getAllCategoriesAndTheirCounts();
        return counts.stream()
                .collect(Collectors.toMap(BookCategoryCount::getSubject,
                        BookCategoryCount::getCount));
    }

    @Override
    public List<String> getAllBookCategories() {
        return bookCategoryRepository.findAllBookCategorySubjects();
    }
}
