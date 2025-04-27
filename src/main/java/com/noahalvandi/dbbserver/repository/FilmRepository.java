package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.Film;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FilmRepository extends JpaRepository<Film, UUID> {

    @EntityGraph(attributePaths = {"filmCategory"})
    Page<Film> findAll(Pageable pageable);
}
