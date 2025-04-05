package com.noahalvandi.dbbserver.util;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PasswordResetToken {
    private Integer userId;
    private LocalDateTime expiresAt;

    public PasswordResetToken(Integer userId, LocalDateTime expiresAt) {
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

}
