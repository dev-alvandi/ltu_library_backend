package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Entity
@Data
public class LoanItem {

    @Id
    @GeneratedValue()
    private UUID loanItemId;

    @ManyToOne
    @JoinColumn(name = "loanId")
    private Loan loan;

    @ManyToOne
    @JoinColumn(name = "bookCopyId")
    private BookCopy bookCopy;

    @ManyToOne
    @JoinColumn(name = "filmCopyId")
    private FilmCopy filmCopy;

    private Date dueDate;
}
