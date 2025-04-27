package com.noahalvandi.dbbserver.util;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class PasswordResetToken {
    private UUID userId;
    private LocalDateTime expiresAt;

    public PasswordResetToken(UUID userId, LocalDateTime expiresAt) {
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

}
