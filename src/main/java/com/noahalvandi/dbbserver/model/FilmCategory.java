package com.noahalvandi.dbbserver.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class FilmCategory {

    @Id
    @GeneratedValue()
    private UUID filmCategoryId;

    private String genre;
}
