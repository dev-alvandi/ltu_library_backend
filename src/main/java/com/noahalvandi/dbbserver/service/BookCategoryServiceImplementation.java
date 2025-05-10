package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.book.BookCategoryCount;
import com.noahalvandi.dbbserver.repository.BookCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookCategoryServiceImplementation implements BookCategoryService {

    private final BookCategoryRepository bookCategoryRepository;

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
