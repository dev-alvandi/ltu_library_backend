package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.model.User;

public interface UserService {

    public User.UserType determineUserTypeByEmail(String email, String firstName, String lastName);
}
