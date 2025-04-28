package com.noahalvandi.dbbserver.dto.request;

import com.noahalvandi.dbbserver.model.user.User;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UserRequest {

    private String firstName;
    private String lastName;

    private String email;

    private String phoneNumber;

    private String city;
    private String street;
    private String postalCode;

}
