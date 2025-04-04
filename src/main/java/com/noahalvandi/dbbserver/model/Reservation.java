package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Entity
@Data
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reservationId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne
    @JoinColumn(name = "bookId")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "filmId")
    private Film film;

    private Date reservedAt;

    @Enumerated(EnumType.ORDINAL)
    private ReservationStatus status;

    @Getter
    public enum ReservationStatus {
        FULFILLED(1),
        CANCELED(0),
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
