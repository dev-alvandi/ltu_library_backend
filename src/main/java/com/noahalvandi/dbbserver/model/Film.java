package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Film {

    @Id
    @GeneratedValue()
    private UUID filmId;

    private String title;
    private String director;
    private int ageRating;
    private String country;
    private String imageURL;

    @ManyToOne
    @JoinColumn(name = "filmCategoryId")
    private FilmCategory filmCategory;
}
