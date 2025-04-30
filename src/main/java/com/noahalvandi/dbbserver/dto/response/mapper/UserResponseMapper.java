package com.noahalvandi.dbbserver.dto.response.mapper;

import com.noahalvandi.dbbserver.dto.response.UserResponse;
import com.noahalvandi.dbbserver.model.User;

public class UserResponseMapper {

    public static UserResponse toDto(User user) {
        UserResponse userResponse = new UserResponse();

        userResponse.setUserId(user.getUserId());

        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());

        userResponse.setDateOfBirth(user.getDateOfBirth());

        userResponse.setPhoneNumber(user.getPhoneNumber());
        userResponse.setCity(user.getCity());
        userResponse.setStreet(user.getStreet());
        userResponse.setPostalCode(user.getPostalCode());

        userResponse.setEmail(user.getEmail());

        userResponse.setUserType(user.getUserType());

        return userResponse;
    }
}
