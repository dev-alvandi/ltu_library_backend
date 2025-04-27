package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.user.User;

public interface UserService {

    public User.UserType determineUserTypeByEmail(String email, String firstName, String lastName);

    public User findUserProfileByJwt(String jwt) throws UserException;

    public boolean isAdminOrLibrarian(User user);

    public boolean isAdmin(User user);

}
