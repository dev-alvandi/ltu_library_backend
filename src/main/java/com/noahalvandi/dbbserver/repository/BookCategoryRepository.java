package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.dto.projection.book.BookCategoryCount;
import com.noahalvandi.dbbserver.model.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, UUID> {

    @Query("""
        SELECT new com.noahalvandi.dbbserver.dto.projection.book.BookCategoryCount(
            b.bookCategory.subject,
            COUNT(b)
        )
        FROM Book b
        GROUP BY b.bookCategory.subject
    """)
    List<BookCategoryCount> getAllCategoriesAndTheirCounts();

    @Query("SELECT COUNT(c) > 0 FROM BookCategory c WHERE LOWER(c.subject) = LOWER(:category)")
    boolean existsBySubjectIgnoreCase(String category);

    Optional<BookCategory> findBySubjectIgnoreCase(String subject);

    @Query("SELECT bc.subject FROM BookCategory bc")
    List<String> findAllBookCategorySubjects();
}
