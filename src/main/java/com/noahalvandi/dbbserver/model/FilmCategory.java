package com.noahalvandi.dbbserver.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class FilmCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int FilmCategoryId;

    private String genre;
}
