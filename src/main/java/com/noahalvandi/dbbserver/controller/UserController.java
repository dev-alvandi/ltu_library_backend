package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.dto.projection.AuthResponse;
import com.noahalvandi.dbbserver.dto.projection.PasswordRequest;
import com.noahalvandi.dbbserver.dto.request.UserRequest;
import com.noahalvandi.dbbserver.dto.response.UserResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.UserResponseMapper;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;

    private final JwtProvider jwtProvider;

    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, UserRepository userRepository, JwtProvider jwtProvider, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @PutMapping("/update-profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @Valid @RequestBody UserRequest request,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {
        User foundUser = userService.findUserProfileByJwt(jwt);
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        if (!foundUser.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.findByEmail(request.getEmail().toLowerCase()) != null) {
            throw new UserException("Email already in use.");
        }

        foundUser.setFirstName(request.getFirstName().toLowerCase());
        foundUser.setLastName(request.getLastName().toLowerCase());
        foundUser.setEmail(request.getEmail().toLowerCase());
        foundUser.setPhoneNumber(request.getPhoneNumber().toLowerCase());
        foundUser.setCity(request.getCity().toLowerCase());
        foundUser.setStreet(request.getStreet().toLowerCase());
        foundUser.setPostalCode(request.getPostalCode().toLowerCase());

        User updatedUser = userRepository.save(foundUser);

        // Manually rebuild Authentication
        Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                updatedUser.getEmail(),
                null,
                currentAuth.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);

        // Generate new JWT
        String newToken = jwtProvider.generateToken(newAuthentication);
        UserResponse userResponse = UserResponseMapper.toDto(updatedUser);

        System.out.println("New token: " + newToken);

        return new ResponseEntity<>(new AuthResponse(newToken, userResponse, true), HttpStatus.OK);
    }


    @PutMapping("/update-password")
    public ResponseEntity<UserResponse> updatePassword(
            @Valid @RequestBody PasswordRequest request,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        // 1. Check if user is authorized (Admin or Librarian)
        if (!userService.isAdminOrLibrarian(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. Validate old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UserException("Old password is incorrect.");
        }

        // 3. Check if new password and confirm match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new UserException("New passwords do not match.");
        }

        // 4. Update password (encode it!)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User updatedUser = userRepository.save(user);

        UserResponse userResponse = UserResponseMapper.toDto(updatedUser);

        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }
}
