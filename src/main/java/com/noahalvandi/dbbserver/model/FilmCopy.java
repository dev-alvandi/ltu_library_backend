package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class FilmCopy {

    @Id
    @GeneratedValue()
    private UUID copyId;

    private String barcode;
    private String physicalLocation;

    @ManyToOne
    @JoinColumn(name = "filmId")
    private Film film;

    @Enumerated(EnumType.ORDINAL)
    private ItemStatus status;;

    @Enumerated(EnumType.ORDINAL)
    private IsItemReferenceCopy isReferenceCopy;
}
