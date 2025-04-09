package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Entity
@Data
public class Loan {

    @Id
    @GeneratedValue()
    private UUID loanId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    private Date loanDate;
}
