package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

@Entity
@Data
public class MagazineCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer magazineCopyId;

    private String barcode;
    private String physicalLocation;

    @ManyToOne
    @JoinColumn(name = "magazineId")
    private Magazine magazine;

    @Enumerated(EnumType.ORDINAL)
    private MagazineCopyStatus status;

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
