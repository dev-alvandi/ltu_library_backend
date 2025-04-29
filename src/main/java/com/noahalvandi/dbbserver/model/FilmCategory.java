package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class FilmCategory {

    @Id
    @GeneratedValue()
    private UUID filmCategoryId;

    @Column(length = 50, nullable = false)
    private String genre;

    @OneToMany(mappedBy = "filmCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Film> films = new ArrayList<>();
}
