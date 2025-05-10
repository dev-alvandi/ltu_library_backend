package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.response.ReservationResponse;
import com.noahalvandi.dbbserver.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReservationService {

    public Page<ReservationResponse> getUserReservations(UUID userId, Pageable pageable);

    public ReservationResponse reserveBookCopy(User user, UUID bookId);

    void deleteReservation(UUID userId, UUID reservationId);

    public void notifyNextUserForBook(UUID bookId);

    public void notifyNextUserForFilm(UUID filmId);

    public void expireAndNotifyNext(UUID reservationId);
}
