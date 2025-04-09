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

    private String title;
    private String issueNumber;
    private Date publicationDate;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "magazineCategoryId")
    private MagazineCategory magazineCategory;
}
