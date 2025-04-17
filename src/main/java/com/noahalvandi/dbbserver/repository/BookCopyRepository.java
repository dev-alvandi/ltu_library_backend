package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.BookCopy;
import org.aspectj.weaver.ast.And;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookCopyRepository extends JpaRepository<BookCopy, UUID> {

    @Query("""
        SELECT COUNT(bc) FROM Book b
            JOIN BookCopy bc ON b.bookId = bc.book.bookId
                WHERE b.bookId = :bookId
                    AND bc.isReferenceCopy = 0
    """)
    int countAllNonReferenceCopiesByBookId(@Param("bookId") UUID bookId);


    @Query("""
        SELECT COUNT(bc) FROM BookCopy bc
            WHERE bc.book.bookId = :bookId
                AND bc.status = 0
                And bc.isReferenceCopy = 0
    """)
    public int numberOfAvailableBookCopiesToBorrow(@Param("bookId") UUID bookId);



    @Query("""
        SELECT bc FROM BookCopy bc
            WHERE bc.bookCopyId = :bookId
                AND bc.status = 0
                And bc.isReferenceCopy = 0
    """)
    public List<BookCopy> findAllAvailableBookCopiesToBorrow(@Param("bookId") UUID bookId);

    // Borrow BookCopy
    @Query("""
    SELECT bc FROM BookCopy bc
        WHERE bc.book.bookId = :bookId
          AND bc.status = 0
          AND bc.isReferenceCopy = 0
        ORDER BY bc.bookCopyId ASC
        LIMIT 1
    """)
    public Optional<BookCopy> findFirstAvailableBookCopy(@Param("bookId") UUID bookId);
}
