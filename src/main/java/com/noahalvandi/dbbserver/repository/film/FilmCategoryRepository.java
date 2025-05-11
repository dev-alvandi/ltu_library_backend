package com.noahalvandi.dbbserver.repository.film;

import com.noahalvandi.dbbserver.dto.projection.film.FilmCategoryCount;
import com.noahalvandi.dbbserver.model.FilmCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilmCategoryRepository extends JpaRepository<FilmCategory, UUID> {
    
    @Query("""
        SELECT new com.noahalvandi.dbbserver.dto.projection.film.FilmCategoryCount(
            f.filmCategory.genre,
            COUNT(f)
        )
        FROM Film f
        GROUP BY f.filmCategory.genre
    """)
    List<FilmCategoryCount> getAllCategoriesAndTheirCounts();

    @Query("SELECT COUNT(c) > 0 FROM FilmCategory c WHERE LOWER(c.genre) = LOWER(:category)")
    boolean existsBySubjectIgnoreCase(String category);

    Optional<FilmCategory> findByGenreIgnoreCase(String genre);

    @Query("SELECT fc.genre FROM FilmCategory fc")
    List<String> findAllFilmCategoryGenres();
}
