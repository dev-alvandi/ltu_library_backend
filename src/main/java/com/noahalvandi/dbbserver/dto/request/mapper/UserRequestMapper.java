package com.noahalvandi.dbbserver.dto.request.mapper;

import com.noahalvandi.dbbserver.dto.request.UserRequest;
import com.noahalvandi.dbbserver.model.User;

public class UserRequestMapper {
    public static User toEntity(UserRequest userRequest) {
        User user = new User();

        user.setFirstName(userRequest.getFirstName().toLowerCase());
        user.setLastName(userRequest.getLastName().toLowerCase());

        user.setEmail(userRequest.getEmail().toLowerCase());

        user.setPhoneNumber(userRequest.getPhoneNumber().toLowerCase());

        user.setCity(userRequest.getCity().toLowerCase());
        user.setStreet(userRequest.getStreet().toLowerCase());
        user.setPostalCode(userRequest.getPostalCode().toLowerCase());

        return user;

    }
}
