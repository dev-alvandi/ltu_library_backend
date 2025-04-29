package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.response.LoanItemResponse;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.BookCopy;
import com.noahalvandi.dbbserver.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    public User.UserType determineUserTypeByEmail(String email, String firstName, String lastName);

    public User findUserProfileByJwt(String jwt) throws UserException;

    public void deleteUserAccount(User user);

    public BookCopy borrowBookCopy(UUID userId, UUID bookId);

    Page<LoanItemResponse> getUserLoanItems(User user, Pageable pageable) throws UserException;

    public boolean isAdminOrLibrarian(User user);

    public boolean isAdmin(User user);

}
