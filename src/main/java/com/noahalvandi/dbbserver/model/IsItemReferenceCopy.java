package com.noahalvandi.dbbserver.model;

import lombok.Getter;

@Getter
public enum IsItemReferenceCopy {
    TRUE(true),
    FALSE(false);

    private final boolean value;

    IsItemReferenceCopy(boolean value) {
        this.value = value;
    }

    public static IsItemReferenceCopy fromValue(boolean value) {
        for (IsItemReferenceCopy type : IsItemReferenceCopy.values()) {
            if (type.isValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid value for IsItemReferenceCopy: " + value);
    }
}
