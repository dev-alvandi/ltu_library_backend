package com.noahalvandi.dbbserver.dto.response;

import com.noahalvandi.dbbserver.model.User;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UserDto {

    private UUID id;

    private String firstName;
    private String lastName;

    private LocalDate dateOfBirth;

    private String phoneNumber;
    private String city;
    private String street;
    private String postalCode;

    private String email;

    private User.UserType userType;
}
