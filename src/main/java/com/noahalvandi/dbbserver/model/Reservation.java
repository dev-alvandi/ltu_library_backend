package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Reservation {
    @Id
    @GeneratedValue()
    private UUID reservationId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "film_id")
    private Film film;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ReservationStatus status;

    @Getter
    public enum ReservationStatus {
        CANCELED(0),
        FULFILLED(1),
        PENDING(2);

        private final int code;

        ReservationStatus(int code) {
            this.code = code;
        }

        public static ReservationStatus fromCode(int code) {
            for (ReservationStatus status : ReservationStatus.values()) {
                if (status.getCode() == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid code for ReservationStatus: " + code);
        }
    }
}
