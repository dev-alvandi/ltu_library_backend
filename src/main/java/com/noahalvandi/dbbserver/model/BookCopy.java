package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class BookCopy {

    @Id
    @GeneratedValue()
    private UUID bookCopyId;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(length = 50)
    private String physicalLocation;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ItemStatus status;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private IsItemReferenceCopy isReferenceCopy;
}
