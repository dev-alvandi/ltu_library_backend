package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.FilmCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilmCopyRepository extends JpaRepository<FilmCopy, UUID> {

    Optional<FilmCopy> findByBarcode(String barcode);

}
