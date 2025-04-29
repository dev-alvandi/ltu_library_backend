package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange;
import com.noahalvandi.dbbserver.dto.projection.BookLanguageCount;
import com.noahalvandi.dbbserver.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    @EntityGraph(attributePaths = {"bookCategory"})
    public Page<Book> findAll(Pageable pageable);

    @Query("SELECT new com.noahalvandi.dbbserver.dto.projection.BookLanguageCount(b.language, COUNT(b)) FROM Book b GROUP BY b.language")
    public List<BookLanguageCount> getAllLanguagesAndTheirCounts();

    @Query("SELECT DISTINCT b.language FROM Book b")
    public List<String> getAllLanguages();

    @Query("SELECT new com.noahalvandi.dbbserver.dto.projection.BooksPublishedYearRange(MIN(b.publishedYear), MAX(b.publishedYear)) FROM Book b")
    public BooksPublishedYearRange getPublishedYearRange();

    @Query("SELECT COUNT(bc) FROM Book b JOIN BookCopy bc ON b.bookId = bc.book.bookId WHERE b.bookId = :bookId")
    public long countAllCopies(@Param("bookId") UUID bookId);

    @Query("""
    SELECT DISTINCT b FROM Book b
        WHERE (
            EXISTS (
                SELECT bc FROM BookCopy bc
                WHERE bc.book = b
                AND bc.status = 0
                AND bc.isReferenceCopy = 0
            )
        )
    """)
    public Page<Book> findAllAvailableBooksToBorrow(Pageable pageable);


    //    Filtered Books
    @Query("""
        SELECT b FROM Book b
        WHERE (
            (:isAvailable IS NULL OR
                (
                    :isAvailable = TRUE AND EXISTS (
                        SELECT bc FROM BookCopy bc WHERE bc.book = b AND bc.status = 0 AND bc.isReferenceCopy = 0
                    )
                ) OR (
                    :isAvailable = FALSE AND NOT EXISTS (
                        SELECT bc FROM BookCopy bc WHERE bc.book = b AND bc.status = 0 AND bc.isReferenceCopy = 0
                    )
                )
            )
        )
        AND (:minYear IS NULL OR b.publishedYear >= :minYear)
        AND (:maxYear IS NULL OR b.publishedYear <= :maxYear)
        AND (:categories IS NULL OR b.bookCategory.subject IN :categories)
        AND (:languages IS NULL OR b.language IN :languages)
    """)
    Page<Book> findBooksByFilters(
            @Param("isAvailable") Boolean isAvailable,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear,
            @Param("categories") List<String> categories,
            @Param("languages") List<String> languages,
            Pageable pageable
    );

//  Get suggestions from the query to match with different attributes
    @Query("SELECT DISTINCT b.title FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctTitlesByQuery(@Param("query") String query);

    @Query("SELECT DISTINCT b.isbn FROM Book b WHERE LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctIsbnsByQuery(@Param("query") String query);

    @Query("SELECT DISTINCT b.author FROM Book b WHERE LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctAuthorsByQuery(@Param("query") String query);

    @Query("SELECT DISTINCT b.publisher FROM Book b WHERE LOWER(b.publisher) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctPublishersByQuery(@Param("query") String query);


    //  Searching Books
    @Query("""
        SELECT b FROM Book b
        WHERE (
            (:query IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR REPLACE(LOWER(b.isbn), '-', '') LIKE REPLACE(LOWER(CONCAT('%', :query, '%')), '-', '')
            OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(b.publisher) LIKE LOWER(CONCAT('%', :query, '%')))
        )
        AND (
            (:isAvailable IS NULL OR
                (
                    :isAvailable = TRUE AND EXISTS (
                        SELECT bc FROM BookCopy bc WHERE bc.book = b AND bc.status = 0 AND bc.isReferenceCopy = 0
                    )
                ) OR (
                    :isAvailable = FALSE AND NOT EXISTS (
                        SELECT bc FROM BookCopy bc WHERE bc.book = b AND bc.status = 0 AND bc.isReferenceCopy = 0
                    )
                )
            )
        )
        AND (:minYear IS NULL OR b.publishedYear >= :minYear)
        AND (:maxYear IS NULL OR b.publishedYear <= :maxYear)
        AND (:categories IS NULL OR b.bookCategory.subject IN :categories)
        AND (:languages IS NULL OR b.language IN :languages)
    """)
    Page<Book> searchWithFilters(
            @Param("query") String query,
            @Param("isAvailable") Boolean isAvailable,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear,
            @Param("categories") List<String> categories,
            @Param("languages") List<String> languages,
            Pageable pageable
    );

    @Query("SELECT b FROM Book b WHERE b.bookId = :bookId")
    Book findBookByBookId(UUID bookId);



    @Modifying
    @Query("DELETE FROM Book b WHERE b.bookId = :bookId")
    void deleteByBookId(@Param("bookId") UUID bookId);

}
