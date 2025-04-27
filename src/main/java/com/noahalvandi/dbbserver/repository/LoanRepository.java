package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {
}
