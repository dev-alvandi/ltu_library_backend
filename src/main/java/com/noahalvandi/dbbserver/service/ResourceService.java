package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.FilterCriteria;
import com.noahalvandi.dbbserver.dto.request.BookCopyRequest;
import com.noahalvandi.dbbserver.dto.request.BookRequest;
import com.noahalvandi.dbbserver.dto.response.BookCopyResponse;
import com.noahalvandi.dbbserver.dto.response.BookResponse;
import com.noahalvandi.dbbserver.dto.response.BookSuggestionsResponse;
import com.noahalvandi.dbbserver.model.BookCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ResourceService {

    public Page<BookResponse> getAllBooks(Pageable pageable);

    public Map<String, Long> getAllLanguagesAndTheirCounts();

    public List<String> getAllLanguages();

    public BooksPublishedYearRange getPublishedYearRange();

    public Page<BookResponse> getFilteredBooks(Pageable pageable, FilterCriteria FilterCriteria);

    public BookSuggestionsResponse getSuggestions(String query);

    public Page<BookResponse> getSearchedBooks(String query, Pageable pageable, FilterCriteria FilterCriteria);

    public BookCopy borrowBookCopy(UUID userId, UUID bookId);

    public BookResponse updateBook(BookRequest bookRequest, MultipartFile image) throws IOException;

    public BookResponse createBook(BookRequest bookRequest, MultipartFile image) throws Exception;

    public BookResponse getRequestedBook(UUID bookId);

    public Page<BookCopyResponse> getBookCopiesByBookId(UUID bookId, Pageable pageable);

    public BookCopyResponse createBookCopy(UUID bookId, BookCopyRequest request) throws Exception;

    public void deleteBookAndCopies(UUID bookId);

    public void deleteBookCopyByBookCopyId(UUID bookCopyId);

    public BookCopyResponse updateBookCopy(UUID bookCopyId, BookCopyRequest request) throws Exception;

}
