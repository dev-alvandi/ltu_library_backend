package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.util.UUID;

@Entity
@Data
public class MagazineCopy {

    @Id
    @GeneratedValue()
    private UUID magazineCopyId;

    @Column(length = 50, nullable = false, unique = true)
    private String barcode;

    @Column(length = 150)
    private String physicalLocation;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private MagazineCopyStatus status;

    @ManyToOne
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private IsItemReferenceCopy isReferenceCopy;

    @Getter
    public enum MagazineCopyStatus {
        AVAILABLE(0),
        LOST(1);

        private final int value;

        MagazineCopyStatus(int value) {
            this.value = value;
        }
    }
}
