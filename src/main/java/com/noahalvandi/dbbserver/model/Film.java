package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int filmId;

    private String title;
    private String director;
    private int ageRating;
    private String country;
    private String imageURL;

    @ManyToOne
    @JoinColumn(name = "filmCategoryId")
    private FilmCategory filmCategory;
}
