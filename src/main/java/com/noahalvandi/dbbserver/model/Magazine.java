package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

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

    @Column(nullable = false, length = 20)
    private String issueNumber;

    @Column(nullable = false)
    private Date publicationDate;

    @Column(nullable = false, length = 100)
    private String language;

    @Column(nullable = false, length = 1000)
    private String image_url;

    @ManyToOne
    @JoinColumn(name = "magazine_category_id")
    private MagazineCategory magazineCategory;
}
