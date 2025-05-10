package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.configuration.RabbitMQConfig;
import com.noahalvandi.dbbserver.dto.response.ReservationResponse;
import com.noahalvandi.dbbserver.exception.ResourceException;
import com.noahalvandi.dbbserver.model.Book;
import com.noahalvandi.dbbserver.model.Film;
import com.noahalvandi.dbbserver.model.Reservation;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.BookRepository;
import com.noahalvandi.dbbserver.repository.LoanRepository;
import com.noahalvandi.dbbserver.repository.ReservationRepository;
import com.noahalvandi.dbbserver.util.EmailTemplates;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationServiceImplementation implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final JavaMailSender mailSender;
    private final S3Service s3Service;
    private final AmqpTemplate amqpTemplate;

    @Override
    public Page<ReservationResponse> getUserReservations(UUID userId, Pageable pageable) {
        Page<Reservation> userReservations = reservationRepository.findByUserUserId(userId, pageable);

        Page<ReservationResponse> reservationResponses = userReservations.map(reservation -> {
            String title = reservation.getBook() != null
                    ? reservation.getBook().getTitle()
                    : reservation.getFilm() != null
                    ? reservation.getFilm().getTitle()
                    : "Unknown";

            String imageUrl = reservation.getBook() != null
                    ? reservation.getBook().getImageUrl()
                    : reservation.getFilm() != null
                    ? reservation.getFilm().getImageUrl()
                    : null;

            int queuePosition = calculateQueuePositionOfReservation(reservation);

            return new ReservationResponse(
                    reservation.getReservationId(),
                    title,
                    imageUrl,  // Set correct imageUrl here!
                    reservation.getReservedAt(),
                    queuePosition
            );
        });

        // Now generate the presigned URLs if imageUrl is not null
        reservationResponses.getContent().forEach(item -> {
            if (item.getImageUrl() != null) {
                String presignedUrl = s3Service.generatePresignedUrl(item.getImageUrl(), GlobalConstants.CLOUD_URL_EXPIRATION_TIME_IN_MINUTES);
                item.setImageUrl(presignedUrl);
            }
        });

        return reservationResponses;
    }

    @Override
    @Transactional
    public ReservationResponse reserveBookCopy(User user, UUID bookId) {
        // Check if book exists
        Book book = bookRepository.findBookByBookId(bookId);
        if (book == null) {
            throw ResourceException.notFound("Book not found.");
        }

        // Check if user has already borrowed the book and not returned it
        boolean alreadyBorrowed = loanRepository.existsByUserIdAndBookIdAndNotReturned(user.getUserId(), bookId);
        if (alreadyBorrowed) {
            throw ResourceException.conflict("You cannot reserve a book you already borrowed.");
        }

        // Check if user has already reserved the same book
        List<Reservation> existingReservations = reservationRepository
                .findByBookBookIdAndStatusOrderByReservedAtAsc(bookId, Reservation.ReservationStatus.PENDING);

        boolean alreadyReserved = existingReservations.stream()
                .anyMatch(reservation -> reservation.getUser().getUserId().equals(user.getUserId()));

        if (alreadyReserved) {
            throw ResourceException.conflict("You have already reserved this book.");
        }

        // Create and save reservation
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setReservedAt(Instant.now());
        reservation.setStatus(Reservation.ReservationStatus.PENDING);

        Reservation savedReservation = reservationRepository.save(reservation);

        scheduleReservationExpiration(savedReservation.getReservationId());

        String imageUrl = savedReservation.getBook().getImageUrl();
        if (imageUrl != null) {
            imageUrl = s3Service.generatePresignedUrl(imageUrl, GlobalConstants.CLOUD_URL_EXPIRATION_TIME_IN_MINUTES);
        }

        return new ReservationResponse(
                savedReservation.getReservationId(),
                savedReservation.getBook().getTitle(),
                imageUrl,
                savedReservation.getReservedAt(),
                calculateQueuePositionOfReservation(savedReservation)
        );
    }

    @Override
    @Transactional
    public void deleteReservation(UUID userId, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> ResourceException.notFound("Reservation not found."));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw ResourceException.forbidden("You are not allowed to delete this reservation.");
        }

        Book book = reservation.getBook();
        Film film = reservation.getFilm();

        reservationRepository.delete(reservation);

        // Handle next reservation in queue
        if (book != null) {
            notifyNextUserForBook(book.getBookId());
        } else if (film != null) {
            notifyNextUserForFilm(film.getFilmId());
        }
    }


    private int calculateQueuePositionOfReservation(Reservation reservation) {
        if (reservation.getBook() != null) {
            List<Reservation> allBookReservations = reservationRepository
                    .findByBookBookIdAndStatusOrderByReservedAtAsc(
                            reservation.getBook().getBookId(),
                            Reservation.ReservationStatus.PENDING
                    );

            return findPositionInQueue(allBookReservations, reservation.getReservationId());
        } else if (reservation.getFilm() != null) {
            List<Reservation> allFilmReservations = reservationRepository
                    .findByFilmFilmIdAndStatusOrderByReservedAtAsc(
                            reservation.getFilm().getFilmId(),
                            Reservation.ReservationStatus.PENDING
                    );

            return findPositionInQueue(allFilmReservations, reservation.getReservationId());
        } else {
            return -1; // should not happen
        }
    }

    private int findPositionInQueue(List<Reservation> reservations, UUID reservationId) {
        for (int i = 0; i < reservations.size(); i++) {
            if (reservations.get(i).getReservationId().equals(reservationId)) {
                return i + 1; // position starts from 1
            }
        }
        return -1;
    }

    @Override
    public void notifyNextUserForBook(UUID bookId) {
        List<Reservation> pending = reservationRepository
                .findByBookBookIdAndStatusOrderByReservedAtAsc(bookId, Reservation.ReservationStatus.PENDING);

        if (!pending.isEmpty()) {
            notifyUserThatTheyCanBorrow(pending.get(0));
        }
    }

    @Override
    public void notifyNextUserForFilm(UUID filmId) {
        List<Reservation> pending = reservationRepository
                .findByFilmFilmIdAndStatusOrderByReservedAtAsc(filmId, Reservation.ReservationStatus.PENDING);

        if (!pending.isEmpty()) {
            notifyUserThatTheyCanBorrow(pending.get(0));
        }
    }

    public void scheduleReservationExpiration(UUID reservationId) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.RESERVATION_EXCHANGE,
                RabbitMQConfig.RESERVATION_ROUTING_KEY,
                reservationId.toString()
        );
    }

    @Override
    @Transactional
    public void expireAndNotifyNext(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null || reservation.getStatus() != Reservation.ReservationStatus.PENDING) return;

        reservation.setStatus(Reservation.ReservationStatus.CANCELED);
        reservationRepository.save(reservation);

        if (reservation.getBook() != null) {
            notifyNextUserForBook(reservation.getBook().getBookId());
        } else if (reservation.getFilm() != null) {
            notifyNextUserForFilm(reservation.getFilm().getFilmId());
        }
    }



    private void notifyUserThatTheyCanBorrow(Reservation reservation) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String userEmail = reservation.getUser().getEmail();
            String userName = reservation.getUser().getFirstName();

            String title = reservation.getBook() != null
                    ? reservation.getBook().getTitle()
                    : reservation.getFilm() != null
                    ? reservation.getFilm().getTitle()
                    : "Unknown Resource";

            helper.setTo(userEmail);
            helper.setSubject("📚 Reserved Resource Now Available");

            String html = EmailTemplates.getReservationAvailableTemplate(userName, title);
            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            System.out.printf("Failed to send reservation availability email to %s: %s", reservation.getUser().getEmail(), e.getMessage());
        }
    }

}
