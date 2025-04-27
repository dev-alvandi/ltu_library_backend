package com.noahalvandi.dbbserver.model;

import lombok.Getter;

@Getter
public enum ItemStatus {
    AVAILABLE(0),
    LOST(1),
    BORROWED(2);

    private final int code;

    ItemStatus(int code) {
        this.code = code;
    }

    public static ItemStatus fromCode(int code) {
        for (ItemStatus status : ItemStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid code for CopyStatus: " + code);
    }

}
