package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.LanguageCount;
import com.noahalvandi.dbbserver.dto.projection.book.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.book.BookFilterCriteria;
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
import com.noahalvandi.dbbserver.repository.BookCategoryRepository;
import com.noahalvandi.dbbserver.repository.BookCopyRepository;
import com.noahalvandi.dbbserver.repository.BookRepository;
import com.noahalvandi.dbbserver.util.BarcodeUtil;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class BookServiceImplementation implements BookService {


    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final S3Service s3Service;

    @Override
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        Page<Book> books = bookRepository.findAllAvailableBooksToBorrow(pageable);
        Page<BookResponse> pageBookDtos = books.map(BookResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(pageBookDtos); // <--- here

        addingAvailableBookCopiesAndNumberOfNonReferenceCopiesToBookResponse(pageBookDtos);

        return pageBookDtos;
    }

    @Override
    public Map<String, Long> getAllLanguagesAndTheirCounts() {
        List<LanguageCount> counts = bookRepository.getAllLanguagesAndTheirCounts();
        return counts.stream()
                .collect(Collectors.toMap(LanguageCount::getLanguage, LanguageCount::getCount));
    }

    @Override
    public List<String> getAllLanguages() {
        return bookRepository.getAllLanguages();
    }

    @Override
    public BooksPublishedYearRange getPublishedYearRange() {
        return bookRepository.getPublishedYearRange();
    }

    @Override
    public Page<BookResponse> getFilteredBooks(Pageable pageable, BookFilterCriteria bookFilterCriteria) {
        Boolean isAvailable = bookFilterCriteria.isAvailable();
        List<String> categories = bookFilterCriteria.getCategories();
        List<String> languages = bookFilterCriteria.getLanguages();

        if (categories != null && categories.isEmpty()) categories = null;
        if (languages != null && languages.isEmpty()) languages = null;

        Page<Book> books = bookRepository.findBooksByFilters(
                isAvailable,
                bookFilterCriteria.getMinYear(),
                bookFilterCriteria.getMaxYear(),
                categories,
                languages,
                pageable
        );

        Page<BookResponse> bookResponses = books.map(BookResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(bookResponses);

        return addingAvailableBookCopiesAndNumberOfNonReferenceCopiesToBookResponse(bookResponses);
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
    public Page<BookResponse> getSearchedBooks(String query, Pageable pageable, BookFilterCriteria filters) {
        Page<BookResponse> bookResponses = bookRepository.searchWithFilters(
                query,
                filters.isAvailable(),
                filters.getMinYear(),
                filters.getMaxYear(),
                filters.getCategories(),
                filters.getLanguages(),
                pageable
        ).map(BookResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(bookResponses);

        return addingAvailableBookCopiesAndNumberOfNonReferenceCopiesToBookResponse(bookResponses);
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
        book.setImageUrl(imageUrl);

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
            String existingImageUrl = book.getImageUrl();
            if (existingImageUrl != null && !existingImageUrl.isBlank()) {
                // Extract key from full URL: everything after the bucket domain
                String key = existingImageUrl.substring(existingImageUrl.indexOf("books/"));
                s3Service.deleteFile(key);
            }

            // Upload new image
            String newImageUrl = uploadBookImage(book.getBookId(), image);
            book.setImageUrl(newImageUrl);
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
            String presignedUrl = s3Service.generatePresignedUrl(fetchedBook.getImageUrl(), GlobalConstants.CLOUD_URL_EXPIRATION_TIME_IN_MINUTES);
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

    @Transactional
    @Override
    public void deleteBookAndCopies(UUID bookId) {
        // Find the Book first
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> ResourceException.notFound("Book not found"));

        // Delete Book Image from S3 if exists
        if (book.getImageUrl() != null && !book.getImageUrl().isBlank()) {
            s3Service.deleteFile(book.getImageUrl());
        }

        // Fetch all BookCopies linked to the Book
        List<BookCopy> bookCopies = bookCopyRepository.findAllByBookBookId(bookId);

        // Delete each barcode image from S3
        for (BookCopy copy : bookCopies) {
            if (copy.getBarcode() != null && !copy.getBarcode().isBlank()) {
                String barcodeKey = String.format(
                        "books/%s/barcodes/%s/%s.png",
                        book.getImageUrl().split("/")[1], // extract the book UUID from image URL
                        copy.getBookCopyId(),
                        copy.getBarcode()
                );
                s3Service.deleteFile(barcodeKey);
            }
        }

        // Delete BookCopies from DB
        bookCopyRepository.deleteBookCopiesByBookId(bookId);

        // Delete the Book itself
        bookRepository.deleteByBookId(bookId);
    }


    @Override
    @Transactional
    public void deleteBookCopyByBookCopyId(UUID bookCopyId) {
        // Find the BookCopy first
        BookCopy bookCopy = bookCopyRepository.findById(bookCopyId)
                .orElseThrow(() -> ResourceException.notFound("BookCopy not found"));

        // Build the S3 barcode key
        String barcodeKey = String.format(
                "books/%s/barcodes/%s/%s.png",
                bookCopy.getBook().getImageUrl().split("/")[1],
                bookCopy.getBookCopyId(),
                bookCopy.getBarcode()
        );

        // Delete the barcode from S3
        s3Service.deleteFile(barcodeKey);

        // Delete the BookCopy from the database
        bookCopyRepository.deleteById(bookCopyId);
    }

    @Override
    @Transactional
    public BookCopyResponse updateBookCopy(UUID bookCopyId, BookCopyRequest request) throws Exception {
        // Fetch the BookCopy
        BookCopy bookCopy = bookCopyRepository.findById(bookCopyId)
                .orElseThrow(() -> ResourceException.notFound("Book copy not found"));

        // Update fields
        if (request.getStatus() != null) {
            bookCopy.setStatus(ItemStatus.valueOf(request.getStatus()));
        }

        bookCopy.setIsReferenceCopy(
                request.isItemReferenceCopy() ? IsItemReferenceCopy.TRUE : IsItemReferenceCopy.FALSE
        );

        if (request.getPhysicalLocation() != null && !request.getPhysicalLocation().isBlank()) {
            bookCopy.setPhysicalLocation(request.getPhysicalLocation());
        }

        // Save the updated BookCopy
        BookCopy updatedCopy = bookCopyRepository.save(bookCopy);

        // Map and return the response
        return BookCopyResponseMapper.toDto(updatedCopy);
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
                book.getImageUrl().split("/")[1],
                bookCopy.getBookCopyId(),
                bookCopy.getBarcode()
        );

        byte[] barcodeImage = BarcodeUtil.generateBarcodePng(bookCopy.getBarcode());
        s3Service.uploadBarcodeImage(key, barcodeImage);
    }

    public String generateBarcodePresignedUrl(Book book, BookCopy bookCopy, int expirationInMinutes) {
        String barcodeKey = String.format(
                "books/%s/barcodes/%s/%s.png",
                book.getImageUrl().split("/")[1],
                bookCopy.getBookCopyId(),
                bookCopy.getBarcode()
        );
        return s3Service.generatePresignedUrl(barcodeKey, expirationInMinutes);
    }

}
