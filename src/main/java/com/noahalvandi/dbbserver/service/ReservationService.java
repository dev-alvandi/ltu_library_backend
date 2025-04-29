package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.response.ReservationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReservationService {

    public Page<ReservationResponse> getUserReservations(UUID userId, Pageable pageable);

}
