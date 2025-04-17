package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class LoanItem {

    @Id
    @GeneratedValue()
    private UUID loanItemId;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;

    @ManyToOne
    @JoinColumn(name = "film_copy_id")
    private FilmCopy filmCopy;

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime dueDate;

    @Column
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime returnedDate;

    @Transient
    public boolean isReturnedLate() {
        return returnedDate != null && returnedDate.isAfter(dueDate);
    }
}
