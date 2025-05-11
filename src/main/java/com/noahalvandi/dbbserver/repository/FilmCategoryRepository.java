package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.FilmCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilmCategoryRepository extends JpaRepository<FilmCategory, UUID> {

    Optional<FilmCategory> findByGenreIgnoreCase(String genre);

    @Query("SELECT fc.genre FROM FilmCategory fc")
    List<String> findAllFilmCategoryGenres();
}
