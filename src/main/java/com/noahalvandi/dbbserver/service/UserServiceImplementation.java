package com.noahalvandi.dbbserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noahalvandi.dbbserver.repository.book.BookCopyRepository;
import com.noahalvandi.dbbserver.repository.film.FilmCopyRepository;
import com.noahalvandi.dbbserver.security.JwtProvider;
import com.noahalvandi.dbbserver.dto.response.LoanResponse;
import com.noahalvandi.dbbserver.dto.response.LoanStatus;
import com.noahalvandi.dbbserver.exception.ResourceException;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.*;
import com.noahalvandi.dbbserver.repository.*;
import com.noahalvandi.dbbserver.util.EmailTemplates;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final LoanRepository loanRepository;
    private final ReservationService reservationService;
    private final BookCopyRepository bookCopyRepository;
    private final FilmCopyRepository filmCopyRepository;
    private final S3Service s3Service;


    @Override
    public User.UserType determineUserTypeByEmail(String email,
                                                  String firstName,
                                                  String lastName) {
        String lowerCaseEmail = email.toLowerCase();
        String domain = lowerCaseEmail.substring(lowerCaseEmail.indexOf("@") + 1);

        if (domain.contains("student")) {
            return User.UserType.STUDENT;
        } else if (domain.contains("ltu.se")) {
            return User.UserType.UNIVERSITY_STAFF;
        }

        try {
            String query = String.format("%s%%20%s", firstName, lastName);
            String url = "https://api.openalex.org/authors?search=" + query;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode results = root.get("results");

                if (results != null && !results.isEmpty()) {
                    return User.UserType.RESEARCHER;
                }
            }


        } catch (Exception e) {
            log.error("Failed to fetch researcher status from OpenAlex for {} {}: {}", firstName, lastName, e.getMessage(), e);
        }


        return User.UserType.PUBLIC;

    }

    @Override
    public void deleteUserAccount(User user) {
        String email = user.getEmail();
        String firstName = capitalize(user.getFirstName());

        userRepository.delete(user);

        try {
            sendAccountDeletionEmail(email, firstName);
        } catch (MessagingException e) {
            log.error("Failed to send account deletion email to {}: {}", email, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public BookCopy borrowBookCopy(User user, UUID bookId) {

        // Check whether the user has reached it active loan limits
        checkActiveLoanLimit(user);

        // Check if the user already borrowed the book
        boolean alreadyBorrowed = loanRepository.existsByUserIdAndBookIdAndNotReturned(user.getUserId(), bookId);
        if (alreadyBorrowed) {
            throw ResourceException.conflict("User already borrowed the book.");
        }

        BookCopy copy = bookCopyRepository.findFirstAvailableBookCopy(bookId)
                .orElseThrow(() -> new ResourceException(HttpStatus.NOT_FOUND, "You may not borrow the same item twice at a time!"));

        int loanDays = copy.getBook().getBookType() == Book.BookType.COURSE_LITERATURE ?
                GlobalConstants.LOAN_DAYS_FOR_COURSE_LITERATURE_BOOKS :
                GlobalConstants.LOAN_DAYS_FOR_NON_COURSE_LITERATURE_BOOKS;

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBookCopy(copy);
        loan.setLoanDate(Instant.now());
        loan.setDueDate(LocalDateTime.now().plusDays(loanDays));
        loanRepository.save(loan);

        copy.setStatus(ItemStatus.BORROWED);
        bookCopyRepository.save(copy);

        // Sending receipt of the Loan transaction via mail:
        try {
            sendLoanReceiptEmail(user, loan, copy);
        } catch (MessagingException e) {
            log.error("Failed to send loan receipt email: {}", e.getMessage(), e);
        }

        return copy;
    }

    @Override
    @Transactional
    public String returnResource(String barcode) {
        LocalDateTime now = LocalDateTime.now();

        // Try book copy first
        BookCopy bookCopy = bookCopyRepository.findByBarcode(barcode).orElse(null);
        if (bookCopy != null) {
            Loan loan = loanRepository.findActiveLoanByBookCopy(bookCopy)
                    .orElseThrow(() -> new ResourceException(HttpStatus.NOT_FOUND, "No active loan found for this book copy."));

            loan.setReturnedDate(now);
            bookCopy.setStatus(ItemStatus.AVAILABLE);

            loanRepository.save(loan);
            bookCopyRepository.save(bookCopy);

            sendReturnReceiptEmail(loan.getUser(), bookCopy.getBook().getTitle(), loan.getDueDate(), now);

            // Notify the next user in reservation queue, if any
            reservationService.notifyNextUserForBook(bookCopy.getBook().getBookId());

            return "Book returned successfully.";
        }

        // Try film copy
        FilmCopy filmCopy = filmCopyRepository.findByBarcode(barcode).orElse(null);
        if (filmCopy != null) {
            Loan loan = loanRepository.findActiveLoanByFilmCopy(filmCopy)
                    .orElseThrow(() -> new ResourceException(HttpStatus.NOT_FOUND, "No active loan found for this film copy."));

            loan.setReturnedDate(now);
            filmCopy.setStatus(ItemStatus.AVAILABLE);

            loanRepository.save(loan);
            filmCopyRepository.save(filmCopy);

            sendReturnReceiptEmail(loan.getUser(), filmCopy.getFilm().getTitle(), loan.getDueDate(), now);

            // Notify the next user in reservation queue, if any
            reservationService.notifyNextUserForFilm(filmCopy.getFilm().getFilmId());

            return "Film returned successfully.";
        }

        throw new ResourceException(HttpStatus.NOT_FOUND, "No resource found with barcode: " + barcode);
    }


    @Override
    @Transactional
    public String extendLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceException(HttpStatus.NOT_FOUND, "Loan not found"));

        if (loan.getReturnedDate() != null) {
            throw ResourceException.conflict("This resource has already been returned.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(loan.getDueDate())) {
            throw ResourceException.conflict("You cannot extend an overdue loan.");
        }

        int baseLoanDays;

        if (loan.getBookCopy() != null) {
            Book book = loan.getBookCopy().getBook();
            baseLoanDays = book.getBookType() == Book.BookType.COURSE_LITERATURE
                    ? GlobalConstants.LOAN_DAYS_FOR_COURSE_LITERATURE_BOOKS
                    : GlobalConstants.LOAN_DAYS_FOR_NON_COURSE_LITERATURE_BOOKS;
        } else if (loan.getFilmCopy() != null) {
            baseLoanDays = GlobalConstants.LOAN_DAYS_FOR_FILMS;
        } else {
            throw ResourceException.badRequest("No associated book or film found on this loan.");
        }

        // Calculate current extension count based on how far dueDate is from loanDate
        Instant loanDate = loan.getLoanDate();
        Instant dueDateAsInstant = loan.getDueDate().atZone(ZoneId.systemDefault()).toInstant();

        long totalDays = Duration.between(loanDate, dueDateAsInstant).toDays();

        int currentMultiplier = (int) (totalDays / baseLoanDays);
        int maxMultiplier = GlobalConstants.MAXIMUM_LOAN_EXTENSION_COUNT + 1;

        if (currentMultiplier >= maxMultiplier) {
            throw ResourceException.conflict("You have reached the maximum number of allowed extensions.");
        }


        loan.setDueDate(loan.getLoanDate().atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime().plusDays((long) baseLoanDays * (currentMultiplier + 1)));
        loanRepository.save(loan);

        return "Loan extended successfully. New due date: " + loan.getDueDate();
    }

    @Override
    public Page<LoanResponse> getUserLoan(User user, Pageable pageable) {
        Page<LoanResponse> bookLoans = loanRepository.findBookLoansByUserId(user.getUserId(), pageable);
        Page<LoanResponse> filmLoans = loanRepository.findFilmLoansByUserId(user.getUserId(), pageable);

        List<LoanResponse> combined = new ArrayList<>();
        combined.addAll(bookLoans.getContent());
        combined.addAll(filmLoans.getContent());

        combined.sort(Comparator.comparing(LoanResponse::getDueAt).reversed());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combined.size());
        List<LoanResponse> pagedList = (start <= end) ? combined.subList(start, end) : new ArrayList<>();

        Map<UUID, Loan> loanMap = fetchLoansByIds(pagedList);

        pagedList.forEach(item -> enrichLoanResponse(item, loanMap.get(item.getLoanId())));

        return new PageImpl<>(pagedList, pageable, combined.size());
    }

    @Override
    public User findUserProfileByJwt(String jwt) throws UserException {
        String email = jwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("No user with the email, \"%s\", found".formatted(email));
        }
        return user;
    }

    @Override
    public boolean isAdminOrLibrarian(User user) {
        return user.getUserType() == User.UserType.ADMIN || user.getUserType() == User.UserType.LIBRARIAN;
    }

    @Override
    public boolean isAdmin(User user) {
        return user.getUserType() == User.UserType.ADMIN;
    }

    private void sendLoanReceiptEmail(User user, Loan loan, BookCopy copy) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(user.getEmail());
        helper.setSubject("📚 Loan Receipt - Luleå University Library");

        String htmlContent = EmailTemplates.getLoanReceiptTemplate(
                user.getFirstName(),
                copy.getBook().getTitle(),
                loan.getLoanDate(),
                loan.getDueDate(),
                GlobalConstants.DAILY_OVERDUE_FEE
        );

        helper.setText(htmlContent, true);
        mailSender.send(mimeMessage);
    }

    private void sendReturnReceiptEmail(User user, String title, LocalDateTime dueDate, LocalDateTime returnedDate) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("📬 Return Confirmation - Luleå University Library");

            boolean isLate = returnedDate.isAfter(dueDate);
            long daysLate = isLate ? java.time.Duration.between(dueDate, returnedDate).toDays() : 0;
            int overdueFee = (int) daysLate * GlobalConstants.DAILY_OVERDUE_FEE;

            String htmlContent = EmailTemplates.getReturnReceiptTemplate(
                    user.getFirstName(),
                    title,
                    returnedDate,
                    dueDate,
                    isLate,
                    overdueFee
            );

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            log.error("Failed to send return receipt email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    private Map<UUID, Loan> fetchLoansByIds(List<LoanResponse> responses) {
        List<UUID> ids = responses.stream().map(LoanResponse::getLoanId).toList();
        return loanRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Loan::getLoanId, Function.identity()));
    }

    private void enrichLoanResponse(LoanResponse item, Loan loan) {
        if (item.getImageUrl() != null) {
            item.setImageUrl(s3Service.generatePresignedUrl(item.getImageUrl(), GlobalConstants.CLOUD_URL_EXPIRATION_TIME_IN_MINUTES));
        }

        LoanStatus status = determineLoanStatus(item);
        item.setStatus(status);

        boolean extendable = switch (status) {
            case RETURNED, OVERDUE -> false;
            case NOT_RETURNED -> isLoanExtendable(item, loan);
        };
        item.setExtendable(extendable);
    }

    private LoanStatus determineLoanStatus(LoanResponse item) {
        return item.isReturned()
                ? LoanStatus.RETURNED
                : (LocalDateTime.now().isAfter(item.getDueAt()) ? LoanStatus.OVERDUE : LoanStatus.NOT_RETURNED);
    }

    private boolean isLoanExtendable(LoanResponse item, Loan loan) {
        if (loan == null) return false;

        int baseLoanDays = 0;
        if (loan.getFilmCopy() != null) {
            baseLoanDays = GlobalConstants.LOAN_DAYS_FOR_FILMS;
        } else if (loan.getBookCopy() != null) {
            Book.BookType type = loan.getBookCopy().getBook().getBookType();
            baseLoanDays = switch (type) {
                case COURSE_LITERATURE -> GlobalConstants.LOAN_DAYS_FOR_COURSE_LITERATURE_BOOKS;
                case PUBLIC -> GlobalConstants.LOAN_DAYS_FOR_NON_COURSE_LITERATURE_BOOKS;
            };
        }

        long totalDays = Duration.between(
                item.getBorrowedAt(),
                item.getDueAt().atZone(ZoneId.systemDefault()).toInstant()
        ).toDays();

        int currentMultiplier = (int) (totalDays / baseLoanDays);
        int maxMultiplier = GlobalConstants.MAXIMUM_LOAN_EXTENSION_COUNT + 1;

        return currentMultiplier < maxMultiplier;
    }

    private void checkActiveLoanLimit(User user) {
        int activeLoanCount = loanRepository.countByUserAndReturnedDateIsNull(user);

        int allowedLimit = switch (user.getUserType()) {
            case ADMIN, LIBRARIAN -> GlobalConstants.MAXIMUM_ACTIVE_LOANS_PER_ADMIN_AND_LIBRARIAN;
            case UNIVERSITY_STAFF -> GlobalConstants.MAXIMUM_ACTIVE_LOANS_PER_UNIVERSITY_STAFF;
            case RESEARCHER -> GlobalConstants.MAXIMUM_ACTIVE_LOANS_PER_RESEARCHER;
            case STUDENT -> GlobalConstants.MAXIMUM_ACTIVE_LOANS_PER_STUDENT;
            case PUBLIC -> GlobalConstants.MAXIMUM_ACTIVE_LOANS_PER_PUBLIC;
        };

        if (activeLoanCount >= allowedLimit) {
            throw ResourceException.conflict(
                    "You have reached your loan limit (%d active loans allowed for %s)."
                            .formatted(allowedLimit, user.getUserType().name().replace('_', ' '))
            );
        }
    }

    private void sendAccountDeletionEmail(String toEmail, String firstName) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Goodbye from Luleå University Library 📚 - Account Deletion Confirmation");

        String htmlContent = EmailTemplates.getAccountDeletionHtmlTemplate(firstName);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }

    private String capitalize(String str) {
        if (str == null || str.isBlank()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
