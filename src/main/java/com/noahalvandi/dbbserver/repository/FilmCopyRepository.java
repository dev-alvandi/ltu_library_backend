package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.FilmCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilmCopyRepository extends JpaRepository<FilmCopy, UUID> {

    Optional<FilmCopy> findByBarcode(String barcode);


    @Query("""
        SELECT COUNT(fc) FROM Film b
            JOIN FilmCopy fc ON b.filmId = fc.film.filmId
                WHERE b.filmId = :filmId
                    AND fc.isReferenceCopy = 0
    """)
    public int countAllNonReferenceCopiesByFilmId(@Param("filmId") UUID filmId);


    @Query("""
        SELECT COUNT(fc) FROM FilmCopy fc
            WHERE fc.film.filmId = :filmId
                AND fc.status = 0
                And fc.isReferenceCopy = 0
    """)
    public int numberOfAvailableFilmCopiesToBorrow(@Param("filmId") UUID filmId);
}
