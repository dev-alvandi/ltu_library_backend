package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.security.JwtProvider;
import com.noahalvandi.dbbserver.dto.request.LoginRequest;
import com.noahalvandi.dbbserver.dto.request.PasswordResetRequest;
import com.noahalvandi.dbbserver.dto.request.RequestPasswordResetRequest;
import com.noahalvandi.dbbserver.dto.request.RegisterRequest;
import com.noahalvandi.dbbserver.dto.response.UserResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.UserResponseMapper;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.dto.projection.AuthResponse;
import com.noahalvandi.dbbserver.service.CustomUserDetailsServiceImplementation;
import com.noahalvandi.dbbserver.service.PasswordResetService;
import com.noahalvandi.dbbserver.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsServiceImplementation customUserDetails;
    private final PasswordResetService passwordResetService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @GetMapping("/debug/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerNewUser(@Valid @RequestBody RegisterRequest request) throws UserException {

        String email = request.getEmail().toLowerCase();

        if (userRepository.findByEmail(email) != null) {
            throw new UserException("User already exists.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().toLowerCase());
        user.setLastName(request.getLastName().toLowerCase());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCity(request.getCity().toLowerCase());
        user.setStreet(request.getStreet().toLowerCase());
        user.setPostalCode(request.getPostalCode().toLowerCase());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType(userService.determineUserTypeByEmail(email, request.getFirstName(), request.getLastName()));

        User savedUser = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);
        UserResponse userResponse = UserResponseMapper.toDto(savedUser);

        return new ResponseEntity<>(new AuthResponse(token, userResponse, true), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) throws UserException {

        String email = request.getEmail().toLowerCase();
        String password = request.getPassword();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Use authenticated principal for token + user lookup
            User foundUser = userRepository.findByEmail(email);
            if (foundUser == null) {
                throw new UserException("User not found");
            }

            UserResponse userResponse = UserResponseMapper.toDto(foundUser);
            String token = jwtProvider.generateToken(authentication);

            return ResponseEntity.ok(new AuthResponse(token, userResponse, true));
        } catch (BadCredentialsException ex) {
            throw new UserException("Invalid email or password");
        }
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody RequestPasswordResetRequest request) {
        System.out.println(request.getEmail());
        passwordResetService.sendPasswordResetToken(request.getEmail());
        return ResponseEntity.ok("Reset link sent if email exists.");
    }

    @PostMapping("/password-reset")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetRequest request) {

        String token = request.getToken();
        String newPassword = request.getPassword();
        String confirmNewPassword = request.getConfirmPassword();

        if (!passwordResetService.isValidToken(token)) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            return ResponseEntity.badRequest().body("Password and confirm password do not match");
        }

        UUID userId = passwordResetService.getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetService.invalidateToken(token);
        return ResponseEntity.ok("Password updated successfully");
    }

    @GetMapping("/is-jwt-valid")
    public ResponseEntity<UserResponse> isJwtValid(@RequestHeader("Authorization") String jwtToken) throws UserException {

        User user = userService.findUserProfileByJwt(jwtToken);

        UserResponse userResponse = UserResponseMapper.toDto(user);

        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.findUserProfileByJwt(jwt);

        if (userService.isAdminOrLibrarian(user)) { // Admin and Librarians may not remove their accounts
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        userService.deleteUserAccount(user);

        return ResponseEntity.ok("Account deleted successfully.");
    }


    private Authentication authenticate(String username, String password) {
        UserDetails userDetails = customUserDetails.loadUserByUsername(username);

        if (userDetails == null) {
            throw new BadCredentialsException("No user with the entered credentials found");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Username or password invalid");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
