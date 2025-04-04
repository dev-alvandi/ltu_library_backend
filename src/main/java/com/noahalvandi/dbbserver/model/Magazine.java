package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class Magazine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer magazineId;

    private String title;
    private String issueNumber;
    private Date publicationDate;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "magazineCategoryId")
    private MagazineCategory magazineCategory;
}
