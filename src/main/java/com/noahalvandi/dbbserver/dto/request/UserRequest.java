package com.noahalvandi.dbbserver.dto.request;

import lombok.Data;

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
