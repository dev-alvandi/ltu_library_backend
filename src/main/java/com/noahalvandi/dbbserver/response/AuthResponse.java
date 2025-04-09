package com.noahalvandi.dbbserver.response;

import com.noahalvandi.dbbserver.dto.response.UserDto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserDto user;
    private boolean status;
}

