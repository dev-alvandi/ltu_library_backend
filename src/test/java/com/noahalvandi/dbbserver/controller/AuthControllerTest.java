package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.security.JwtProvider;
import com.noahalvandi.dbbserver.dto.request.LoginRequest;
import com.noahalvandi.dbbserver.dto.request.PasswordResetRequest;
import com.noahalvandi.dbbserver.dto.request.RequestPasswordResetRequest;
import com.noahalvandi.dbbserver.dto.request.RegisterRequest;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.dto.projection.AuthResponse;
import com.noahalvandi.dbbserver.service.CustomUserDetailsServiceImplementation;
import com.noahalvandi.dbbserver.service.PasswordResetService;
import com.noahalvandi.dbbserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomUserDetailsServiceImplementation customUserDetails;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;


    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerNewUser_shouldReturnTokenAndUserDto_whenRequestIsValid() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "Password123!";
        String encodedPassword = "ENCODED";
        String jwtToken = "jwt.token.example";

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Noah");
        request.setLastName("Alvandi");
        request.setEmail(email);
        request.setPassword(password);
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));
        request.setPhoneNumber("0701234567");
        request.setCity("Stockholm");
        request.setStreet("Main Street");
        request.setPostalCode("12345");

        when(userRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        User savedUser = new User();
        savedUser.setEmail(email);
        savedUser.setFirstName("noah");
        savedUser.setLastName("alvandi");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(userService.determineUserTypeByEmail(
                eq(email),
                eq(request.getFirstName()),
                eq(request.getLastName())
        )).thenReturn(User.UserType.STUDENT); // Assume you have this enum or class

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtProvider.generateToken(authentication)).thenReturn(jwtToken);

        // Act
        ResponseEntity<AuthResponse> response = authController.registerNewUser(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        AuthResponse authResponse = response.getBody();
        assertNotNull(authResponse);
        assertTrue(authResponse.isStatus());
        assertEquals(jwtToken, authResponse.getToken());
        assertEquals(email, authResponse.getUser().getEmail());
    }

    @Test
    public void shouldReturnToken_WhenCredentialsAreCorrect() throws Exception {
        // Arrange
        String email = "user@example.com";
        String password = "securePassword";
        String jwt = "mocked.jwt.token";

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        User user = new User();
        user.setEmail(email);
        user.setFirstName("Noah");
        user.setLastName("Alvandi");

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(jwtProvider.generateToken(authentication)).thenReturn(jwt);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthResponse authResponse = response.getBody();
        assertNotNull(authResponse);
        assertTrue(authResponse.isStatus());
        assertEquals(jwt, authResponse.getToken());
        assertEquals(email, authResponse.getUser().getEmail());
    }

    @Test
    public void shouldThrowBadCredentialsException_WhenPasswordIsInvalid() {
        // Arrange
        String email = "test@example.com";
        String password = "wrongpass";

        LoginRequest user = new LoginRequest();
        user.setEmail(email);
        user.setPassword(password);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("realencodedpassword")
                .authorities(new ArrayList<>())
                .build();

        when(customUserDetails.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(password, "realencodedpassword")).thenReturn(false);

        // Act & Assert
        assertThrows(UserException.class, () -> authController.login(user));
    }

    @Test
    public void shouldThrowBadCredentialsException_WhenUserNotFound() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "pass";

        LoginRequest user = new LoginRequest();
        user.setEmail(email);
        user.setPassword(password);

        when(customUserDetails.loadUserByUsername(email)).thenReturn(null);

        // Act & Assert
        assertThrows(UserException.class, () -> authController.login(user));
    }

    @Test
    void testRequestPasswordReset() {
        // Given
        RequestPasswordResetRequest request = new RequestPasswordResetRequest();
        request.setEmail("test@example.com");

        // When
        ResponseEntity<String> response = authController.requestPasswordReset(request);

        // Then
        verify(passwordResetService).sendPasswordResetToken("test@example.com");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Reset link sent if email exists.", response.getBody());
    }

    @Test
    void testResetPassword_withValidTokenAndUser() {
        // Given
        String token = "valid-token";
        String newPassword = "new-password";
        UUID userId = UUID.randomUUID();

        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken(token);
        request.setPassword(newPassword);
        request.setConfirmPassword(newPassword);

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        when(passwordResetService.isValidToken(token)).thenReturn(true);
        when(passwordResetService.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("hashed-password");

        // When
        ResponseEntity<String> response = authController.resetPassword(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password updated successfully", response.getBody());

        verify(userRepository).save(user);
        assertEquals("hashed-password", user.getPassword());
        verify(passwordResetService).invalidateToken(token);
    }

    @Test
    void testResetPassword_withInvalidToken() {

        // Given
        String token = "invalid-token";
        String newPassword = "new-password";
        UUID userId = UUID.randomUUID();

        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken(token);
        request.setPassword(newPassword);
        request.setConfirmPassword(newPassword);

        when(passwordResetService.isValidToken("invalid-token")).thenReturn(false);

        // When
        ResponseEntity<String> response = authController.resetPassword(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid or expired token", response.getBody());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPassword_userNotFound() {
        // Given
        UUID missingUserId = UUID.randomUUID();
        String token = "valid-token";
        String newPassword = "new-password";

        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken(token);
        request.setPassword(newPassword);
        request.setConfirmPassword(newPassword);

        when(passwordResetService.isValidToken(token)).thenReturn(true);
        when(passwordResetService.getUserIdFromToken(token)).thenReturn(missingUserId);
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        // When
        ResponseEntity<String> response = authController.resetPassword(request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());
    }

}