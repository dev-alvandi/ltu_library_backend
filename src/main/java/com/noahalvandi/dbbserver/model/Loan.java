package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int loanId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    private Date loanDate;
}
