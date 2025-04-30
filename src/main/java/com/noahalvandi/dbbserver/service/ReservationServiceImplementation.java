package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.response.ReservationResponse;
import com.noahalvandi.dbbserver.model.Reservation;
import com.noahalvandi.dbbserver.repository.ReservationRepository;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationServiceImplementation implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final S3Service s3Service;

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

            int queuePosition = calculateQueuePosition(reservation);

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

    private int calculateQueuePosition(Reservation reservation) {
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
}
