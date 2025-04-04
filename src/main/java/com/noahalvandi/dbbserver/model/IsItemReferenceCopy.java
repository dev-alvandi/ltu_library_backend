package com.noahalvandi.dbbserver.model;

import lombok.Getter;

@Getter
public enum IsItemReferenceCopy {
    TRUE(1),
    FALSE(0);

    private final int code;

    IsItemReferenceCopy(int code) {
        this.code = code;
    }

    public static IsItemReferenceCopy fromCode(int code) {
        for (IsItemReferenceCopy type : IsItemReferenceCopy.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code for IsReferenceCopy: " + code);
    }
}
