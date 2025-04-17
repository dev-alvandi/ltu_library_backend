package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.FilterCriteria;
import com.noahalvandi.dbbserver.dto.response.BookResponse;
import com.noahalvandi.dbbserver.dto.response.BookSuggestionsResponse;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.BookCopy;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.service.BookCategoryService;
import com.noahalvandi.dbbserver.service.ResourceService;
import com.noahalvandi.dbbserver.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    private final BookCategoryService bookCategoryService;

    private final UserService userService;

    public ResourceController(ResourceService resourceService, BookCategoryService bookCategoryService, UserService userService) {
        this.resourceService = resourceService;
        this.bookCategoryService = bookCategoryService;
        this.userService = userService;
    }

    @GetMapping("/books")
    public ResponseEntity<Page<BookResponse>> allBooks(@PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BookResponse> books = resourceService.getAllBooks(pageable);
        return new ResponseEntity<>(books, HttpStatus.OK);
    }

    @GetMapping("/books-categories")
    public ResponseEntity<Map<String, Long>> allBooksCategories() {

        Map<String, Long> bookCategories = bookCategoryService.getAllBooksCategoriesAndTheirCounts();

        return new ResponseEntity<>(bookCategories, HttpStatus.OK);
    }

    @GetMapping("/books-languages")
    public ResponseEntity<Map<String, Long>> allBooksLanguages() {

        Map<String, Long> languagesCounts = resourceService.getAllLanguagesAndTheirCounts();

        return new ResponseEntity<>(languagesCounts, HttpStatus.OK);
    }

    @GetMapping("/books-year-range")
    public ResponseEntity<BooksPublishedYearRange> allBooksYearRange() {

        BooksPublishedYearRange booksPublishedYearRange = resourceService.getPublishedYearRange();

        return new ResponseEntity<>(booksPublishedYearRange, HttpStatus.OK);
    }

    @GetMapping("/books-filters")
    public ResponseEntity<?> allBooksFilters() {

        BooksPublishedYearRange yeaRange = resourceService.getPublishedYearRange();
        Map<String, Long> categories = bookCategoryService.getAllBooksCategoriesAndTheirCounts();
        Map<String, Long> languages = resourceService.getAllLanguagesAndTheirCounts();

        Map<String, Object> response = new HashMap<>();
        response.put("publishedYearRange", yeaRange);
        response.put("categories", categories);
        response.put("languages", languages);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/filtered-books")
    public ResponseEntity<Page<BookResponse>> getFilteredBooks(
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> languages,
            @PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable
    ) {

        FilterCriteria filter = new FilterCriteria(
                isAvailable, minYear, maxYear, categories, languages
        );

        Page<BookResponse> filteredBooks = resourceService.getFilteredBooks(pageable, filter);
        return new ResponseEntity<>(filteredBooks, HttpStatus.OK);
    }

    @GetMapping("/suggested-books")
    public ResponseEntity<BookSuggestionsResponse> getSuggestions(@RequestParam String query) {
        BookSuggestionsResponse response = resourceService.getSuggestions(query);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/searched-books")
    public ResponseEntity<Page<BookResponse>> getSearchedBook(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> languages,
            @PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        System.out.println("Query " + query);
        FilterCriteria filters = new FilterCriteria(isAvailable, minYear, maxYear, categories, languages);
        Page<BookResponse> result = resourceService.getSearchedBooks(query, pageable, filters);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/borrow/{bookId}")
    public ResponseEntity<BookCopy> borrowBookCopy(@PathVariable UUID bookId, @RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        BookCopy borrowedCopy = resourceService.borrowBookCopy(user.getUserId() ,bookId);

        System.out.println("BorrowedCopy " + borrowedCopy);

        return new ResponseEntity<>(borrowedCopy, HttpStatus.CREATED) ;
    }
}
