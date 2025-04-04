package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int copyId;

    private String barcode;
    private String physicalLocation;

    @ManyToOne
    @JoinColumn(name = "bookId")
    private Book book;

    @Enumerated(EnumType.ORDINAL)
    private ItemStatus status;;

    @Enumerated(EnumType.ORDINAL)
    private IsItemReferenceCopy isReferenceCopy;
}
