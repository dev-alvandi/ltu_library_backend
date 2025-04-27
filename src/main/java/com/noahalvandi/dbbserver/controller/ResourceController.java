package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.FilterCriteria;
import com.noahalvandi.dbbserver.dto.request.BookCopyRequest;
import com.noahalvandi.dbbserver.dto.request.BookRequest;
import com.noahalvandi.dbbserver.dto.response.BookCopyResponse;
import com.noahalvandi.dbbserver.dto.response.BookResponse;
import com.noahalvandi.dbbserver.dto.response.BookSuggestionsResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.BookResponseMapper;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.Book;
import com.noahalvandi.dbbserver.model.BookCopy;
import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.BookCopyRepository;
import com.noahalvandi.dbbserver.repository.BookRepository;
import com.noahalvandi.dbbserver.service.BookCategoryService;
import com.noahalvandi.dbbserver.service.ResourceService;
import com.noahalvandi.dbbserver.service.UserService;
import com.noahalvandi.dbbserver.util.BarcodeUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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

    private final BookRepository bookRepository;

    private final BookCopyRepository bookCopyRepository;

    public ResourceController(ResourceService resourceService, BookCategoryService bookCategoryService, UserService userService, BookRepository bookRepository, BookCopyRepository bookCopyRepository) {
        this.resourceService = resourceService;
        this.bookCategoryService = bookCategoryService;
        this.userService = userService;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
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



    @GetMapping("/book/{bookId}")
    public ResponseEntity<BookResponse> getRequestedBook(@PathVariable UUID bookId,
                                                         @RequestHeader("Authorization") String jwt) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        BookResponse fetchedBook = resourceService.getRequestedBook(bookId);

        if (fetchedBook == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(fetchedBook);
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

        Page<BookCopyResponse> bookCopies = resourceService.getBookCopiesByBookId(bookId, pageable);

        return new ResponseEntity<>(bookCopies, HttpStatus.OK);
    }

    @PostMapping("/book-copy/{bookId}/create")
    public ResponseEntity<BookCopyResponse> createBookCopy(
            @PathVariable UUID bookId,
            @RequestBody BookCopyRequest request,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {

        System.out.println("Request " + request);

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookCopyResponse createdCopy = resourceService.createBookCopy(bookId, request);


        return new ResponseEntity<>(createdCopy, HttpStatus.CREATED);
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
            @RequestHeader("Authorization") String jwt
    ) throws Exception {

        System.out.println("Image: " + (image != null ? image.getOriginalFilename() : "null"));

        User user = userService.findUserProfileByJwt(jwt);
        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookRequest bookRequest = new BookRequest(title, author, publisher, publishedYear,
                isbn, language, Book.BookType.valueOf(bookType), category);

        BookResponse created = resourceService.createBook(bookRequest, image);
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


        BookResponse created = resourceService.updateBook(bookRequest, image);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

}
