package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.Reservation;
import com.noahalvandi.dbbserver.model.Reservation.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Page<Reservation> findByUserUserId(UUID userId, Pageable pageable);

    List<Reservation> findByBookBookIdAndStatusOrderByReservedAtAsc(UUID bookId, ReservationStatus status);

    List<Reservation> findByFilmFilmIdAndStatusOrderByReservedAtAsc(UUID filmId, ReservationStatus status);
}
