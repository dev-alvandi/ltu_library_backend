package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Loan {

    @Id
    @GeneratedValue()
    private UUID loanId;

    private LocalDateTime loanDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

//    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<LoanItem> loanItems = new ArrayList<>();
}
