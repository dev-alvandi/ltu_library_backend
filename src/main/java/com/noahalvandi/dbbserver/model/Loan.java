package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Loan {

    @Id
    @GeneratedValue()
    private UUID loanId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;

    @ManyToOne
    @JoinColumn(name = "film_copy_id")
    private FilmCopy filmCopy;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(nullable = false)
    private Instant loanDate;

    @Column
    private LocalDateTime returnedDate;

    @Transient
    public boolean isReturnedLate() {
        return returnedDate != null && returnedDate.isAfter(dueDate);
    }
}
