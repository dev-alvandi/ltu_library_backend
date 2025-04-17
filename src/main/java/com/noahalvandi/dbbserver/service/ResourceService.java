package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.FilterCriteria;
import com.noahalvandi.dbbserver.dto.response.BookResponse;
import com.noahalvandi.dbbserver.dto.response.BookSuggestionsResponse;
import com.noahalvandi.dbbserver.model.BookCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ResourceService {

    public Page<BookResponse> getAllBooks(Pageable pageable);

    public Map<String, Long> getAllLanguagesAndTheirCounts();

    public BooksPublishedYearRange getPublishedYearRange();

    public Page<BookResponse> getFilteredBooks(Pageable pageable, FilterCriteria FilterCriteria);

    public BookSuggestionsResponse getSuggestions(String query);

    public Page<BookResponse> getSearchedBooks(String query, Pageable pageable, FilterCriteria FilterCriteria);

    public BookCopy borrowBookCopy(UUID userId, UUID bookId);
}
