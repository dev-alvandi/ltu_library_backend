package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.dto.projection.BookCategoryCount;
import com.noahalvandi.dbbserver.dto.projection.LanguageBookCount;
import com.noahalvandi.dbbserver.model.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BookCategoryRepository extends JpaRepository<BookCategory, UUID> {

    @Query("""
        SELECT new com.noahalvandi.dbbserver.dto.projection.BookCategoryCount(
            b.bookCategory.subject,
            COUNT(b)
        )
        FROM Book b
        GROUP BY b.bookCategory.subject
    """)
    List<BookCategoryCount> getAllCategoriesAndTheirCounts();
}
