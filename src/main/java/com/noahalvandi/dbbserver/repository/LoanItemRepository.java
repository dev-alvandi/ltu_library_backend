package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.dto.response.LoanItemResponse;
import com.noahalvandi.dbbserver.model.LoanItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
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

    // BookCopy-based loan items
    @Query("""
        SELECT new com.noahalvandi.dbbserver.dto.response.LoanItemResponse(
            li.loanItemId,
            b.imageUrl,
            b.title,
            l.loanDate,
            li.dueDate,
            CASE WHEN li.returnedDate IS NOT NULL THEN true ELSE false END
        )
        FROM LoanItem li
        JOIN li.loan l
        JOIN li.bookCopy bc
        JOIN bc.book b
        WHERE l.user.userId = :userId
    """)
    Page<LoanItemResponse> findBookLoanItemsByUserId(@Param("userId") UUID userId, Pageable pageable);


    // FilmCopy-based loan items
    @Query("""
        SELECT new com.noahalvandi.dbbserver.dto.response.LoanItemResponse(
            li.loanItemId,
            f.imageUrl,
            f.title,
            l.loanDate,
            li.dueDate,
            CASE WHEN li.returnedDate IS NOT NULL THEN true ELSE false END
        )
        FROM LoanItem li
        JOIN li.loan l
        JOIN li.filmCopy fc
        JOIN fc.film f
        WHERE l.user.userId = :userId
    """)
    Page<LoanItemResponse> findFilmLoanItemsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
