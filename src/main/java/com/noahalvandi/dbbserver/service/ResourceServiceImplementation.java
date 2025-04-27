package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.FilterCriteria;
import com.noahalvandi.dbbserver.dto.projection.LanguageBookCount;
import com.noahalvandi.dbbserver.dto.request.BookCopyRequest;
import com.noahalvandi.dbbserver.dto.request.BookRequest;
import com.noahalvandi.dbbserver.dto.request.mapper.BookRequestMapper;
import com.noahalvandi.dbbserver.dto.response.BookCopyResponse;
import com.noahalvandi.dbbserver.dto.response.BookResponse;
import com.noahalvandi.dbbserver.dto.response.BookSuggestionsResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.BookCopyResponseMapper;
import com.noahalvandi.dbbserver.dto.response.mapper.BookResponseMapper;
import com.noahalvandi.dbbserver.exception.ResourceException;
import com.noahalvandi.dbbserver.model.*;
import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.*;
import com.noahalvandi.dbbserver.util.BarcodeUtil;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ResourceServiceImplementation implements ResourceService {


    private final BookRepository bookRepository;

    private final BookCopyRepository bookCopyRepository;

    private final UserRepository userRepository;

    private final LoanRepository loanRepository;

    private final LoanItemRepository loanItemRepository;

    private final BookCategoryRepository bookCategoryRepository;

    private final S3Service s3Service;

    public ResourceServiceImplementation(BookRepository bookRepository,
                                         BookCopyRepository bookCopyRepository,
                                         UserRepository userRepository,
                                         LoanRepository loanRepository,
                                         LoanItemRepository loanItemRepository,
                                         BookCategoryRepository bookCategoryRepository,
                                         S3Service s3Service) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.loanItemRepository = loanItemRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.s3Service = s3Service;
    }

    @Override
    public Page<BookResponse> getAllBooks(Pageable pageable) {

        Page<Book> books = bookRepository.findAll(pageable);

        Page<BookResponse> pageBookDtos = books.map(BookResponseMapper::toDto);

        pageBookDtos.getContent().forEach(bookResponse -> {
            System.out.println(bookResponse.getImageUrl());
            if (bookResponse.getImageUrl() != null) {
                String presignedUrl = s3Service.generatePresignedUrl(bookResponse.getImageUrl(), 5); // 5 minutes
                bookResponse.setImageUrl(presignedUrl);
            }

        });

        addingAvailableBookCopiesAndNumberOfNonReferenceCopiesToBookResponse(pageBookDtos);

        return pageBookDtos;
    }

    @Override
    public Map<String, Long> getAllLanguagesAndTheirCounts() {
        List<LanguageBookCount> counts = bookRepository.getAllLanguagesAndTheirCounts();
        return counts.stream()
                .collect(Collectors.toMap(LanguageBookCount::getLanguage, LanguageBookCount::getCount));
    }

    @Override
    public BooksPublishedYearRange getPublishedYearRange() {
        return bookRepository.getPublishedYearRange();
    }

    @Override
    public Page<BookResponse> getFilteredBooks(Pageable pageable, FilterCriteria filterCriteria) {
        Boolean isAvailable = filterCriteria.isAvailable();
        List<String> categories = filterCriteria.getCategories();
        List<String> languages = filterCriteria.getLanguages();

        // Convert empty lists to null to match query logic
        if (categories != null && categories.isEmpty()) categories = null;
        if (languages != null && languages.isEmpty()) languages = null;

        Page<Book> books = bookRepository.findBooksByFilters(
                isAvailable,
                filterCriteria.getMinYear(),
                filterCriteria.getMaxYear(),
                categories,
                languages,
                pageable
        );

        System.out.println(books.toString());

        Page<BookResponse> bookResponses = books.map(BookResponseMapper::toDto);

        // Enrich with copy counts
        Page<BookResponse> modifiedBookResponse = addingAvailableBookCopiesAndNumberOfNonReferenceCopiesToBookResponse(bookResponses);

        return modifiedBookResponse;
    }

    @Override
    public BookSuggestionsResponse getSuggestions(String query) {
        BookSuggestionsResponse bookSuggestionsResponse = new BookSuggestionsResponse();

        bookSuggestionsResponse.setTitle(bookRepository.findDistinctTitlesByQuery(query));
        bookSuggestionsResponse.setIsbn(bookRepository.findDistinctIsbnsByQuery(query));
        bookSuggestionsResponse.setAuthor(bookRepository.findDistinctAuthorsByQuery(query));
        bookSuggestionsResponse.setPublisher(bookRepository.findDistinctPublishersByQuery(query));

        return bookSuggestionsResponse;
    }


    @Override
    public Page<BookResponse> getSearchedBooks(String query, Pageable pageable, FilterCriteria filters) {
        Page<BookResponse> bookResponses = bookRepository.searchWithFilters(
                query,
                filters.isAvailable(),
                filters.getMinYear(),
                filters.getMaxYear(),
                filters.getCategories(),
                filters.getLanguages(),
                pageable
        ).map(BookResponseMapper::toDto);

        // Enrich with copy counts
        Page<BookResponse> modifiedBookResponse = addingAvailableBookCopiesAndNumberOfNonReferenceCopiesToBookResponse(bookResponses);

        return modifiedBookResponse;
    }

    private Page<BookResponse> addingAvailableBookCopiesAndNumberOfNonReferenceCopiesToBookResponse(Page<BookResponse> bookResponses) {
        bookResponses.getContent().forEach(bookResponse -> {
            bookResponse.setNumberOfCopies(
                    bookCopyRepository.countAllNonReferenceCopiesByBookId(bookResponse.getBookId())
            );
            bookResponse.setNumberOfAvailableToBorrowCopies(
                    bookCopyRepository.numberOfAvailableBookCopiesToBorrow(bookResponse.getBookId())
            );
        });

        return bookResponses;
    }

    @Override
    public BookCopy borrowBookCopy(UUID userId, UUID bookId) {
        // Check if the user already borrowed a book
        List<LoanItem> activeLoans = loanItemRepository.findActiveBookLoanByUserAndBook(userId, bookId);

        if (!activeLoans.isEmpty()) {
            throw ResourceException.conflict("User already borrowed the book.");
        }

        BookCopy copy = bookCopyRepository.findFirstAvailableBookCopy(bookId)
                .orElseThrow(() -> new ResourceException(HttpStatus.NOT_FOUND, "You may not borrow the same item twice at a time!"));

        User foundUser = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Determine loan period
        int loanDays = copy.getBook().getBookType() == Book.BookType.COURSE_LITERATURE ?
                GlobalConstants.LOAN_DAYS_FOR_COURSE_LITERATURE_BOOKS :
                GlobalConstants.LOAN_DAYS_FOR_NON_COURSE_LITERATURE_BOOKS;

        // Create Loan
        Loan loan = new Loan();
        loan.setUser(foundUser);
        loan.setLoanDate(LocalDateTime.now());
        loanRepository.save(loan);

        // Create LoanItem
        LoanItem loanItem = new LoanItem();
        loanItem.setLoan(loan);
        loanItem.setBookCopy(copy);
        loanItem.setDueDate(LocalDateTime.now().plusDays(loanDays));
        loanItemRepository.save(loanItem);

        // Update BookCopy status
        copy.setStatus(ItemStatus.BORROWED); // or status = 2
        bookCopyRepository.save(copy);

        return copy;
    }



    @Override
    public BookResponse createBook(BookRequest bookRequest, MultipartFile image) throws Exception {

        // 1. Handle BookCategory
        BookCategory bookCategory = bookCategoryRepository.findBySubjectIgnoreCase(bookRequest.getBookCategory())
                .orElseGet(() -> {
                    BookCategory newCategory = new BookCategory();
                    newCategory.setSubject(bookRequest.getBookCategory());
                    return bookCategoryRepository.save(newCategory);
                });

        // 2. Upload image to S3 (if present)
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            UUID tempId = UUID.randomUUID(); // temporary ID to construct S3 key
            imageUrl = uploadBookImage(tempId, image);
        }

        // 3. Save Book
        Book book = BookRequestMapper.toEntity(bookRequest);


        book.setBookCategory(bookCategory);
        book.setImage_url(imageUrl);

        Book savedBook = bookRepository.save(book);


        // 4. Create one available BookCopy
        BookCopy bookCopy = new BookCopy();
        bookCopy.setBook(savedBook);
        bookCopy.setPhysicalLocation("Inventory");
        bookCopy.setIsReferenceCopy(IsItemReferenceCopy.valueOf("TRUE"));
        bookCopy.setStatus(ItemStatus.AVAILABLE);

        // Generate Barcode
        String barcodeText = "BOOK-" +
                book.getBookId().toString().substring(0, 8) + "-" +
                UUID.randomUUID().toString().substring(0, 8);

        bookCopy.setBarcode(barcodeText);

        BookCopy savedBookCopy = bookCopyRepository.save(bookCopy);

        uploadBookBarcodeImage(savedBook, savedBookCopy);


        // 5. Return mapped DTO
        BookResponse response = BookResponseMapper.toDto(book);
        response.setNumberOfCopies(1);
        response.setNumberOfAvailableToBorrowCopies(1);

        return response;
    }

    @Override
    public BookResponse updateBook(BookRequest bookRequest, MultipartFile image) throws IOException {
        Book book = bookRepository.findById(bookRequest.getBookId())
                .orElseThrow(() -> ResourceException.notFound("Book not found"));

        // Find or create a category
        BookCategory bookCategory = bookCategoryRepository.findBySubjectIgnoreCase(bookRequest.getBookCategory())
                .orElseGet(() -> {
                    BookCategory newBookCategory = new BookCategory();
                    newBookCategory.setSubject(bookRequest.getBookCategory());
                    return bookCategoryRepository.save(newBookCategory);
                });

        // Handle image upload if a new image is provided
        if (image != null && !image.isEmpty()) {
            // Delete the old image from S3 if it exists
            String existingImageUrl = book.getImage_url();
            if (existingImageUrl != null && !existingImageUrl.isBlank()) {
                // Extract key from full URL: everything after the bucket domain
                String key = existingImageUrl.substring(existingImageUrl.indexOf("books/"));
                s3Service.deleteFile(key);
            }

            // Upload new image
            String newImageUrl = uploadBookImage(book.getBookId(), image);
            book.setImage_url(newImageUrl);
        }


        // Update fields
        book.setTitle(bookRequest.getTitle());
        book.setAuthor(bookRequest.getAuthor());
        book.setIsbn(bookRequest.getIsbn());
        book.setPublisher(bookRequest.getPublisher());
        book.setPublishedYear(Integer.parseInt(bookRequest.getPublishedYear()));
        book.setLanguage(bookRequest.getLanguage());
        book.setBookType(bookRequest.getBookType());
        book.setBookCategory(bookCategory);

        bookRepository.save(book);

        BookResponse response = BookResponseMapper.toDto(book);
        response.setNumberOfCopies(bookCopyRepository.countAllNonReferenceCopiesByBookId(book.getBookId()));
        response.setNumberOfAvailableToBorrowCopies(bookCopyRepository.numberOfAvailableBookCopiesToBorrow(book.getBookId()));

        return response;
    }

    @Override
    public BookResponse getRequestedBook(UUID bookId) {
        Book book = bookRepository.findBookByBookId(bookId);

        BookResponse fetchedBook = BookResponseMapper.toDto(book);

        if (fetchedBook.getImageUrl() != null) {
            String presignedUrl = s3Service.generatePresignedUrl(fetchedBook.getImageUrl(), 5); // 5 minutes
            fetchedBook.setImageUrl(presignedUrl);
        }

        return fetchedBook;
    }


    @Override
    public Page<BookCopyResponse> getBookCopiesByBookId(UUID bookId, Pageable pageable) {
        // Fetch the Book entity first
        Book book = bookRepository.findBookByBookId(bookId);

        if (book == null) {
            throw ResourceException.notFound("Book not found");
        }

        // Fetch BookCopy entities
        Page<BookCopy> bookCopies = bookCopyRepository.findBookCopiesByBookId(bookId, pageable);

        // Map and enrich each BookCopy
        Page<BookCopyResponse> bookCopyResponses = bookCopies.map(bookCopy -> {
            BookCopyResponse response = BookCopyResponseMapper.toDto(bookCopy);

            if (bookCopy.getBarcode() != null) {
                String barcodeUrl = generateBarcodePresignedUrl(book, bookCopy, 5); // 5 minutes expiry
                response.setBarcodeUrl(barcodeUrl);
            }

            return response;
        });

        return bookCopyResponses;
    }

    @Override
    public BookCopyResponse createBookCopy(UUID bookId, BookCopyRequest request) throws Exception {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> ResourceException.notFound("Book not found"));

        System.out.println("Found Book " + book.toString());
        System.out.println("BookCopyRequest " + request.toString());

        BookCopy newCopy = new BookCopy();
        newCopy.setBook(book);
        newCopy.setPhysicalLocation(request.getPhysicalLocation());
        newCopy.setIsReferenceCopy(
                request.isItemReferenceCopy() ? IsItemReferenceCopy.TRUE : IsItemReferenceCopy.FALSE
        );
        newCopy.setStatus(ItemStatus.AVAILABLE);

        // Generate new barcode
        String barcodeText = "BOOK-" +
                book.getBookId().toString().substring(0, 8) +
                "-" +
                UUID.randomUUID().toString().substring(0, 8);

        newCopy.setBarcode(barcodeText);

        BookCopy savedCopy = bookCopyRepository.save(newCopy);

        // Upload barcode to S3
        uploadBookBarcodeImage(book, savedCopy);

        return BookCopyResponseMapper.toDto(savedCopy);
    }

    private String uploadBookImage(UUID bookId, MultipartFile file) throws IOException {

        String key = "books/" + bookId + "/" + file.getOriginalFilename();
        s3Service.uploadFile(key, file);
        return key;
    }

    private void uploadBookBarcodeImage(Book book, BookCopy bookCopy) throws Exception {
//        String sanitizedTitle = book.getTitle().replaceAll("[^a-zA-Z0-9\\-_]", "_"); // safe for S3 key
        String key = String.format(
                "books/%s/barcodes/%s/%s.png",
                book.getImage_url().split("/")[1],
                bookCopy.getBookCopyId(),
                bookCopy.getBarcode()
        );

        byte[] barcodeImage = BarcodeUtil.generateBarcodePng(bookCopy.getBarcode());
        s3Service.uploadBarcodeImage(key, barcodeImage);
    }

    public String generateBarcodePresignedUrl(Book book, BookCopy bookCopy, int expirationInMinutes) {
        String barcodeKey = String.format(
                "books/%s/barcodes/%s/%s.png",
                book.getImage_url().split("/")[1],
                bookCopy.getBookCopyId(),
                bookCopy.getBarcode()
        );
        return s3Service.generatePresignedUrl(barcodeKey, expirationInMinutes);
    }

}
