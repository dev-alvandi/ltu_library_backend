package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.UUID;

@Entity
@Data
public class Magazine {

    @Id
    @GeneratedValue()
    private UUID magazineId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 20, unique = true)
    private String issueNumber;

    @Column(nullable = false)
    private Date publicationDate;

    @Column(nullable = false, length = 100)
    private String language;

    @Column(name = "image_url", nullable = false, length = 1000, unique = true)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "magazine_category_id")
    @ToString.Exclude
    private MagazineCategory magazineCategory;
}
