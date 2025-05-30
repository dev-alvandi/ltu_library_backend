package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Film {

    @Id
    @GeneratedValue()
    private UUID filmId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 100)
    private String director;

    @Column(nullable = false)
    private LocalDate releasedDate;

    @Column(nullable = false)
    private int ageRating;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String language;

    @Column(name = "image_url", nullable = false, length = 1000, unique = true)
    private String imageUrl;

    @ManyToOne()
    @JoinColumn(name = "film_category_id", nullable = false)
    @ToString.Exclude
    private FilmCategory filmCategory;

    @OneToMany(mappedBy = "film", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @ToString.Exclude
    private List<FilmCopy> filmCopies = new ArrayList<>();
//
//    @OneToMany(mappedBy = "film", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Reservation> reservations = new ArrayList<>();
}
