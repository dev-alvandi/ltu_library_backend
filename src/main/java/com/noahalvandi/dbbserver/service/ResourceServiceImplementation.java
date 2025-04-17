package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.FilterCriteria;
import com.noahalvandi.dbbserver.dto.projection.LanguageBookCount;
import com.noahalvandi.dbbserver.dto.response.BookResponse;
import com.noahalvandi.dbbserver.dto.response.BookSuggestionsResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.BookResponseMapper;
import com.noahalvandi.dbbserver.exception.ResourceException;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.*;
import com.noahalvandi.dbbserver.repository.*;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class ResourceServiceImplementation implements ResourceService {


    private final BookRepository bookRepository;

    private final BookCopyRepository bookCopyRepository;

    private final UserRepository userRepository;

    private final LoanRepository loanRepository;

    private final LoanItemRepository loanItemRepository;

    public ResourceServiceImplementation(BookRepository bookRepository, BookCopyRepository bookCopyRepository, UserRepository userRepository, LoanRepository loanRepository, LoanItemRepository loanItemRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.loanItemRepository = loanItemRepository;
    }
    @Override
    public Page<BookResponse> getAllBooks(Pageable pageable) {

        Page<Book> books = bookRepository.findAll(pageable);

        Page<BookResponse> pageBookDtos = books.map(BookResponseMapper::toDto);

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
            throw ResourceException.conflict("User already has borrowed the same book.");
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
}
