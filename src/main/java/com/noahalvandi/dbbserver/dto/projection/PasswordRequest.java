package com.noahalvandi.dbbserver.dto.projection;

import lombok.Data;

@Data
public class PasswordRequest {

    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
}

