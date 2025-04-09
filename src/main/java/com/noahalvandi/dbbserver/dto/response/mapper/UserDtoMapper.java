package com.noahalvandi.dbbserver.dto.response.mapper;

import com.noahalvandi.dbbserver.dto.response.UserDto;
import com.noahalvandi.dbbserver.model.User;

public class UserDtoMapper {

    public static UserDto toDto(User user) {
        UserDto userDto = new UserDto();

        userDto.setId(userDto.getId());

        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());

        userDto.setDateOfBirth(user.getDateOfBirth());

        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setCity(user.getCity());
        userDto.setStreet(user.getStreet());
        userDto.setPostalCode(user.getPostalCode());

        userDto.setEmail(user.getEmail());

        userDto.setUserType(user.getUserType());

        return userDto;
    }
}
