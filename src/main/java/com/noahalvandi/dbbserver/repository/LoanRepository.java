package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.dto.response.LoanResponse;
import com.noahalvandi.dbbserver.model.BookCopy;
import com.noahalvandi.dbbserver.model.FilmCopy;
import com.noahalvandi.dbbserver.model.Loan;
import com.noahalvandi.dbbserver.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    @Query("""
        SELECT new com.noahalvandi.dbbserver.dto.response.LoanResponse(
            l.loanId,
            b.imageUrl,
            b.title,
            l.loanDate,
            l.dueDate,
            CASE WHEN l.returnedDate IS NOT NULL THEN true ELSE false END
        )
        FROM Loan l
        JOIN l.bookCopy bc
        JOIN bc.book b
        WHERE l.user.userId = :userId
    """)
    Page<LoanResponse> findBookLoansByUserId(@Param("userId") UUID userId, Pageable pageable);


    @Query("""
        SELECT new com.noahalvandi.dbbserver.dto.response.LoanResponse(
            l.loanId,
            f.imageUrl,
            f.title,
            l.loanDate,
            l.dueDate,
            CASE WHEN l.returnedDate IS NOT NULL THEN true ELSE false END
        )
        FROM Loan l
        JOIN l.filmCopy fc
        JOIN fc.film f
        WHERE l.user.userId = :userId
    """)
    Page<LoanResponse> findFilmLoansByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT COUNT(l) > 0
        FROM Loan l
        WHERE l.user.userId = :userId
          AND l.bookCopy.book.bookId = :bookId
          AND l.returnedDate IS NULL
    """)
    boolean existsByUserIdAndBookIdAndNotReturned(@Param("userId") UUID userId, @Param("bookId") UUID bookId);

    @Query("""
        SELECT COUNT(l)
        FROM Loan l
        WHERE l.user = :user AND l.returnedDate IS NULL
    """)
    int countByUserAndReturnedDateIsNull(@Param("user") User user);


    @Query("""
        SELECT l FROM Loan l
        WHERE l.bookCopy = :bookCopy AND l.returnedDate IS NULL
    """)
    Optional<Loan> findActiveLoanByBookCopy(@Param("bookCopy") BookCopy bookCopy);

    @Query("""
        SELECT l FROM Loan l
        WHERE l.filmCopy = :filmCopy AND l.returnedDate IS NULL
    """)
    Optional<Loan> findActiveLoanByFilmCopy(@Param("filmCopy") FilmCopy filmCopy);


}
