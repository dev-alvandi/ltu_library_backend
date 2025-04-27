package com.noahalvandi.dbbserver.dto.projection;

import com.noahalvandi.dbbserver.dto.response.UserResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserResponse user;
    private boolean status;
}

