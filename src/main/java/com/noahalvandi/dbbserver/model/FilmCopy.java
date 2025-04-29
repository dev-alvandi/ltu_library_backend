package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.UUID;

@Entity
@Data
public class FilmCopy {

    @Id
    @GeneratedValue()
    private UUID filmCopyId;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(length = 150)
    private String physicalLocation;

    @ManyToOne
    @JoinColumn(name = "film_id", nullable = false)
    @ToString.Exclude
    private Film film;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ItemStatus status;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private IsItemReferenceCopy isReferenceCopy;
}
