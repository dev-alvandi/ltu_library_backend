package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.dto.projection.LanguagesCategories;
import com.noahalvandi.dbbserver.dto.projection.book.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.book.BookFilterCriteria;
import com.noahalvandi.dbbserver.dto.request.book.BookCopyRequest;
import com.noahalvandi.dbbserver.dto.request.book.BookRequest;
import com.noahalvandi.dbbserver.dto.response.book.BookCopyResponse;
import com.noahalvandi.dbbserver.dto.response.book.BookResponse;
import com.noahalvandi.dbbserver.dto.response.book.BookSuggestionsResponse;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.Book;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.service.book.BookCategoryService;
import com.noahalvandi.dbbserver.service.book.BookService;
import com.noahalvandi.dbbserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookCategoryService bookCategoryService;
    private final UserService userService;


    @GetMapping("/books")
    public ResponseEntity<Page<BookResponse>> allBooks(@PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BookResponse> books = bookService.getAllBooks(pageable);
        return new ResponseEntity<>(books, HttpStatus.OK);
    }

    @GetMapping("/books-categories-and-counts")
    public ResponseEntity<Map<String, Long>> allBooksCategoriesAndCounts() {

        Map<String, Long> bookCategories = bookCategoryService.getAllBooksCategoriesAndTheirCounts();

        return new ResponseEntity<>(bookCategories, HttpStatus.OK);
    }

    @GetMapping("/books-categories-and-languages")
    public ResponseEntity<LanguagesCategories> allBooksCategoriesAndLanguages() {

        List<String> bookCategories = bookCategoryService.getAllBookCategories();
        List<String> bookLanguages = bookService.getAllLanguages();

        LanguagesCategories blc = new LanguagesCategories();

        blc.setCategories(bookCategories);
        blc.setLanguages(bookLanguages);

        return new ResponseEntity<>(blc, HttpStatus.OK);
    }

    @GetMapping("/books-categories")
    public ResponseEntity<List<String>> allBooksCategories(@RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        List<String> bookCategories = bookCategoryService.getAllBookCategories();

        return new ResponseEntity<>(bookCategories, HttpStatus.OK);
    }

    @GetMapping("/books-languages")
    public ResponseEntity<List<String>> allBooksLanguages(@RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        List<String> bookCategories = bookService.getAllLanguages();

        return new ResponseEntity<>(bookCategories, HttpStatus.OK);
    }

    @GetMapping("/books-languages-and-counts")
    public ResponseEntity<Map<String, Long>> allBooksLanguagesAndCounts() {

        Map<String, Long> languagesCounts = bookService.getAllLanguagesAndTheirCounts();

        return new ResponseEntity<>(languagesCounts, HttpStatus.OK);
    }

    @GetMapping("/books-year-range")
    public ResponseEntity<BooksPublishedYearRange> allBooksYearRange() {

        BooksPublishedYearRange booksPublishedYearRange = bookService.getPublishedYearRange();

        return new ResponseEntity<>(booksPublishedYearRange, HttpStatus.OK);
    }

    @GetMapping("/books-filters")
    public ResponseEntity<?> allBooksFilters() {

        BooksPublishedYearRange yeaRange = bookService.getPublishedYearRange();
        Map<String, Long> categories = bookCategoryService.getAllBooksCategoriesAndTheirCounts();
        Map<String, Long> languages = bookService.getAllLanguagesAndTheirCounts();

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
            @PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {

        BookFilterCriteria filter = new BookFilterCriteria(
                isAvailable, minYear, maxYear, categories, languages
        );

        Page<BookResponse> filteredBooks = bookService.getFilteredBooks(pageable, filter);
        return new ResponseEntity<>(filteredBooks, HttpStatus.OK);
    }

    @GetMapping("/suggested-books")
    public ResponseEntity<BookSuggestionsResponse> getSuggestions(@RequestParam String query) {

        BookSuggestionsResponse response = bookService.getSuggestions(query);

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
            @PageableDefault(page = 0, size = 9, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {

        BookFilterCriteria filters = new BookFilterCriteria(isAvailable, minYear, maxYear, categories, languages);

        Page<BookResponse> result = bookService.getSearchedBooks(query, pageable, filters);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<BookResponse> getRequestedBook(
            @PathVariable UUID bookId,
            @RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        BookResponse fetchedBook = bookService.getRequestedBook(bookId);

        if (fetchedBook == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(fetchedBook);
    }

    @DeleteMapping("/book/{bookId}")
    public ResponseEntity<String> deleteBook(
            @PathVariable UUID bookId,
            @RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        bookService.deleteBookAndCopies(bookId);

        return new ResponseEntity<>("Book deleted", HttpStatus.NO_CONTENT);
    }

    @GetMapping("/book/{bookId}/book-copies")
    public ResponseEntity<Page<BookCopyResponse>> getBookCopies(
                @PathVariable UUID bookId,
                @RequestHeader("Authorization") String jwt,
                @PageableDefault(page = 0, size = 5) Pageable pageable) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<BookCopyResponse> bookCopies = bookService.getBookCopiesByBookId(bookId, pageable);

        return new ResponseEntity<>(bookCopies, HttpStatus.OK);
    }

    @PostMapping("/book-copy/{bookId}/create")
    public ResponseEntity<BookCopyResponse> createBookCopy(
            @PathVariable UUID bookId,
            @RequestBody BookCopyRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookCopyResponse createdCopy = bookService.createBookCopy(bookId, request);


        return new ResponseEntity<>(createdCopy, HttpStatus.CREATED);
    }

    @DeleteMapping("/book-copy/{bookCopyId}")
    public ResponseEntity<String> deleteBookCopy(
            @PathVariable UUID bookCopyId,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        bookService.deleteBookCopyByBookCopyId(bookCopyId);

        return new ResponseEntity<>("Book Copy deleted", HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/book/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookResponse> createBook(
            @RequestPart("title") String title,
            @RequestPart("isbn") String isbn,
            @RequestPart("author") String author,
            @RequestPart("publisher") String publisher,
            @RequestPart("publishedYear") String publishedYear,
            @RequestPart("bookType") String bookType,
            @RequestPart("language") String language,
            @RequestPart("category") String category,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);
        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookRequest bookRequest = new BookRequest(title, author, publisher, publishedYear,
                isbn, language, Book.BookType.valueOf(bookType), category);

        BookResponse created = bookService.createBook(bookRequest, image);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    @PutMapping(value = "/book/{bookId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable UUID bookId,
            @RequestPart("title") String title,
            @RequestPart("isbn") String isbn,
            @RequestPart("author") String author,
            @RequestPart("publisher") String publisher,
            @RequestPart("publishedYear") String publishedYear,
            @RequestPart("bookType") String bookType,
            @RequestPart("language") String language,
            @RequestPart("category") String category,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String jwt) throws UserException, IOException {

        User user = userService.findUserProfileByJwt(jwt);
        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookRequest bookRequest = new BookRequest(bookId, title, author, publisher, publishedYear,
                isbn, language, Book.BookType.valueOf(bookType), category);


        BookResponse created = bookService.updateBook(bookRequest, image);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping(value = "/book-copy/{bookCopyId}/update")
    public ResponseEntity<BookCopyResponse> updateBookCopy(
            @PathVariable UUID bookCopyId,
            @RequestBody BookCopyRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookCopyResponse bookCopyResponse = bookService.updateBookCopy(bookCopyId, request);

        return new ResponseEntity<>(bookCopyResponse, HttpStatus.CREATED);
    }
}
