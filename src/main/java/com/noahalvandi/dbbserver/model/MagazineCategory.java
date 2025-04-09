package com.noahalvandi.dbbserver.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class MagazineCategory {

    @Id
    @GeneratedValue()
    private UUID magazineCategoryId;

    private String subject;
}
