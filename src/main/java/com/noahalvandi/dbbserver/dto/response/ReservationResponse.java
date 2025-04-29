package com.noahalvandi.dbbserver.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ReservationResponse {
    private UUID reservationId;
    private String title;
    private String imageUrl;
    private Instant reservedAt;
    private int queuePosition;
}
