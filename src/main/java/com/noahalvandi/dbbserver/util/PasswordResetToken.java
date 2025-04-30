package com.noahalvandi.dbbserver.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PasswordResetToken {

    private final UUID userId;
    private final LocalDateTime expiresAt;

}
