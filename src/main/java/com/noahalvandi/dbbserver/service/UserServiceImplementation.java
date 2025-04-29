package com.noahalvandi.dbbserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.dto.response.LoanItemResponse;
import com.noahalvandi.dbbserver.dto.response.LoanStatus;
import com.noahalvandi.dbbserver.exception.ResourceException;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.*;
import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.BookCopyRepository;
import com.noahalvandi.dbbserver.repository.LoanItemRepository;
import com.noahalvandi.dbbserver.repository.LoanRepository;
import com.noahalvandi.dbbserver.repository.UserRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final LoanItemRepository loanItemRepository;
    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;
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
    public Page<LoanItemResponse> getUserLoanItems(User user, Pageable pageable) throws UserException {
        // Fetch pages separately
        Page<LoanItemResponse> bookLoanItems = loanItemRepository.findBookLoanItemsByUserId(user.getUserId(), pageable);
        Page<LoanItemResponse> filmLoanItems = loanItemRepository.findFilmLoanItemsByUserId(user.getUserId(), pageable);

        // Merge both Page contents
        List<LoanItemResponse> combinedItems = new ArrayList<>();
        combinedItems.addAll(bookLoanItems.getContent());
        combinedItems.addAll(filmLoanItems.getContent());

        // Sort all by dueAt descending
        combinedItems.sort(Comparator.comparing(LoanItemResponse::getDueAt).reversed());

        // Manual pagination (since original pages are separate)
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedItems.size());
        List<LoanItemResponse> pagedList;

        if (start <= end) {
            pagedList = combinedItems.subList(start, end);
        } else {
            pagedList = new ArrayList<>();
        }

        // Apply S3 URL and loan status logic
        pagedList.forEach(item -> {
            if (item.getImageUrl() != null) {
                String presignedUrl = s3Service.generatePresignedUrl(item.getImageUrl(), 5); // 5 minutes
                item.setImageUrl(presignedUrl);
            }

            LoanStatus status;
            LocalDateTime now = LocalDateTime.now();
            if (item.isReturned()) {
                status = LoanStatus.RETURNED;
            } else if (now.isAfter(item.getDueAt())) {
                status = LoanStatus.OVERDUE;
            } else {
                status = LoanStatus.NOT_RETURNED;
            }
            item.setStatus(status);
        });

        return new PageImpl<>(pagedList, pageable, combinedItems.size());
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
}
