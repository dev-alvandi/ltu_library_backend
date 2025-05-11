package com.noahalvandi.dbbserver.repository.film;

import com.noahalvandi.dbbserver.dto.projection.LanguageCount;
import com.noahalvandi.dbbserver.dto.projection.film.FilmsReleasedDateRange;
import com.noahalvandi.dbbserver.model.Film;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface FilmRepository extends JpaRepository<Film, UUID> {

    @EntityGraph(attributePaths = {"filmCategory"})
    public Page<Film> findAll(Pageable pageable);

    @Query("SELECT new com.noahalvandi.dbbserver.dto.projection.LanguageCount(f.language, COUNT(f)) FROM Film f GROUP BY f.language")
    public List<LanguageCount> getAllLanguagesAndTheirCounts();

    @Query("SELECT DISTINCT f.language FROM Film f")
    public List<String> getAllLanguages();
    
    @Query("SELECT new com.noahalvandi.dbbserver.dto.projection.film.FilmsReleasedDateRange(MIN(f.releasedDate), MAX(f.releasedDate)) FROM Film f")
    public FilmsReleasedDateRange getFilmsReleasedDateRange();

    @Query("""
        SELECT DISTINCT f FROM Film f
        WHERE EXISTS (
            SELECT fc FROM FilmCopy fc
            WHERE fc.film = f
            AND fc.status = 0
            AND fc.isReferenceCopy = 0
        )
    """)
    public Page<Film> findAllAvailableFilmsToBorrow(Pageable pageable);

    //    Filtered Films
    @Query("""
        SELECT f FROM Film f
        WHERE (
            (:isAvailable IS NULL OR
                (
                    :isAvailable = TRUE AND EXISTS (
                        SELECT fc FROM FilmCopy fc WHERE fc.film = f AND fc.status = 0 AND fc.isReferenceCopy = 0
                    )
                ) OR (
                    :isAvailable = FALSE AND NOT EXISTS (
                        SELECT fc FROM FilmCopy fc WHERE fc.film = f AND fc.status = 0 AND fc.isReferenceCopy = 0
                    )
                )
            )
        )
        AND (:minDate IS NULL OR f.releasedDate >= :minDate)
        AND (:maxDate IS NULL OR f.releasedDate <= :maxDate)
        AND (:categories IS NULL OR f.filmCategory.genre IN :categories)
        AND (:languages IS NULL OR f.language IN :languages)
        AND (:ageRating IS NULL OR f.ageRating <= :ageRating)
    """)
    Page<Film> findFilmsByFilters(
            @Param("isAvailable") Boolean isAvailable,
            @Param("minDate") Date minDate,
            @Param("maxDate") Date maxDate,
            @Param("categories") List<String> categories,
            @Param("languages") List<String> languages,
            @Param("ageRating") Integer ageRating,
            Pageable pageable
    );

//    //  Get suggestions from the query to match with different attributes
    @Query("SELECT DISTINCT f.title FROM Film f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctTitlesByQuery(@Param("query") String query);

    @Query("SELECT DISTINCT f.director FROM Film f WHERE LOWER(f.director) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctDirectorsByQuery(@Param("query") String query);

    @Query("SELECT DISTINCT f.country FROM Film f WHERE LOWER(f.country) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctCountriesByQuery(@Param("query") String query);

    @Query("""
        SELECT f FROM Film f
        WHERE (
            :query IS NULL OR
            LOWER(f.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
            REPLACE(LOWER(f.director), '-', '') LIKE REPLACE(LOWER(CONCAT('%', :query, '%')), '-', '') OR
            LOWER(f.country) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (
            :isAvailable IS NULL OR
            (
                :isAvailable = TRUE AND EXISTS (
                    SELECT fc FROM FilmCopy fc WHERE fc.film = f AND fc.status = 0 AND fc.isReferenceCopy = 0
                )
            ) OR (
                :isAvailable = FALSE AND NOT EXISTS (
                    SELECT fc FROM FilmCopy fc WHERE fc.film = f AND fc.status = 0 AND fc.isReferenceCopy = 0
                )
            )
        )
        AND (:minDate IS NULL OR f.releasedDate >= :minDate)
        AND (:maxDate IS NULL OR f.releasedDate <= :maxDate)
        AND (:categories IS NULL OR f.filmCategory.genre IN :categories)
        AND (:languages IS NULL OR f.language IN :languages)
        AND (:ageRating IS NULL OR f.ageRating <= :ageRating)
    """)
    Page<Film> searchWithFilters(
            @Param("query") String query,
            @Param("isAvailable") Boolean isAvailable,
            @Param("minDate") Date minDate,
            @Param("maxDate") Date maxDate,
            @Param("categories") List<String> categories,
            @Param("languages") List<String> languages,
            @Param("ageRating") Integer ageRating,
            Pageable pageable
    );

    @Query("SELECT f FROM Film f WHERE f.filmId = :filmId")
    Film findFilmByFilmId(UUID filmId);
    
    @Modifying
    @Query("DELETE FROM Film b WHERE b.filmId = :filmId")
    void deleteByFilmId(@Param("filmId") UUID filmId);
}
