package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class MagazineCategory {

    @Id
    @GeneratedValue()
    private UUID magazineCategoryId;

    @Column(length = 50, nullable = false)
    private String subject;

//    @OneToMany(mappedBy = "magazineCategory")
//    private List<Magazine> magazines = new ArrayList<>();
}
