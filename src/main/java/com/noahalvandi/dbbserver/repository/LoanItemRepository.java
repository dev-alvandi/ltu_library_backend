package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.LoanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LoanItemRepository extends JpaRepository<LoanItem, UUID> {

    @Query("""
    SELECT li FROM LoanItem li
        JOIN li.loan l
        WHERE l.user.userId = :userId
          AND li.bookCopy.status = 2
          AND li.bookCopy.book.bookId = :bookId
    """)
    List<LoanItem> findActiveBookLoanByUserAndBook(
            @Param("userId") UUID userId,
            @Param("bookId") UUID bookId
    );
}
