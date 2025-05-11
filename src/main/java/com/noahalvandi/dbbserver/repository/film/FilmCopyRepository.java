package com.noahalvandi.dbbserver.repository.film;

import com.noahalvandi.dbbserver.model.FilmCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilmCopyRepository extends JpaRepository<FilmCopy, UUID> {

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

//     // Borrow FilmCopy
    @Query("""
    SELECT fc FROM FilmCopy fc
        WHERE fc.film.filmId = :filmId
          AND fc.status = 0
          AND fc.isReferenceCopy = 0
        ORDER BY fc.filmCopyId ASC
        LIMIT 1
    """)
    public Optional<FilmCopy> findFirstAvailableFilmCopy(@Param("filmId") UUID filmId);
    
    @Query("SELECT fc FROM FilmCopy fc WHERE fc.film.filmId = :filmId ORDER BY fc.isReferenceCopy DESC")
    Page<FilmCopy> findFilmCopiesByFilmId(@Param("filmId") UUID filmId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM FilmCopy fc WHERE fc.film.filmId = :filmId")
    void deleteFilmCopiesByFilmId(@Param("filmId") UUID filmId);
    
    List<FilmCopy> findAllByFilmFilmId(UUID filmId);
    
    Optional<FilmCopy> findByBarcode(String barcode);
}
