package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
}
