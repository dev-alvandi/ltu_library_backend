package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.response.AuthResponse;
import com.noahalvandi.dbbserver.service.CustomUserDetailsServiceImplementation;
import com.noahalvandi.dbbserver.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerNewUser_shouldCreateNewUserAndReturnToken() throws UserException {
        // Arrange
        User requestUser = new User();
        requestUser.setFirstName("Noah");
        requestUser.setLastName("Alvandi");
        requestUser.setEmail("mohghi-3@student.ltu.se");
        requestUser.setPassword("securePassword123");
        requestUser.setPhoneNumber("0701234567");
        requestUser.setCity("Stockholm");
        requestUser.setStreet("Main Street");
        requestUser.setPostalCode("12345");

        String hashedPassword = "SOME_RANDOM_STRING_THAT_REPRESENTS_HASHED_PASSWORD";
        String jwtToken = "MOCKED_JWT_TOKEN";

        when(userRepository.findByEmail(requestUser.getEmail())).thenReturn(null);
        when(passwordEncoder.encode(requestUser.getPassword())).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        when(jwtProvider.generateToken(any(Authentication.class))).thenReturn(jwtToken);

        // Act
        ResponseEntity<AuthResponse> response = authController.registerNewUser(requestUser);

        // Assert
        assertEquals(201, response.getStatusCode().value());
        AuthResponse authResponse = response.getBody();
        assertNotNull(authResponse);
        assertTrue(authResponse.isStatus());
        assertEquals(jwtToken, authResponse.getJwt());

        // Verify that the password was encoded before saving
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(hashedPassword, userCaptor.getValue().getPassword());

        verify(jwtProvider).generateToken(any(Authentication.class));
    }

    @Test
    public void shouldReturnToken_WhenCredentialsAreCorrect() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String encodedPassword = "encoded123";
        String token = "mock-jwt-token";

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password(encodedPassword)
                .authorities(new ArrayList<>())
                .build();

        when(customUserDetails.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(jwtProvider.generateToken(any())).thenReturn(token);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(user);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        AuthResponse authResponse = response.getBody();
        assertNotNull(authResponse);
        assertTrue(authResponse.isStatus());
        assertEquals(token, response.getBody().getJwt());
    }

    @Test
    public void shouldThrowBadCredentialsException_WhenPasswordIsInvalid() {
        // Arrange
        String email = "test@example.com";
        String password = "wrongpass";

        User user = new User();
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
        assertThrows(BadCredentialsException.class, () -> authController.login(user));
    }

    @Test
    public void shouldThrowBadCredentialsException_WhenUserNotFound() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "pass";

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);

        when(customUserDetails.loadUserByUsername(email)).thenReturn(null);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authController.login(user));
    }

    @Test
    void testRequestPasswordReset() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");

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
        int userId = 1;

        Map<String, String> request = Map.of(
                "token", token,
                "password", newPassword,
                "confirmPassword", newPassword
        );

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
        Map<String, String> request = Map.of(
                "token", "invalid-token",
                "newPassword", "somePassword"
        );

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
        String token = "valid-token";
        int missingUserId = 999;

        Map<String, String> request = Map.of(
                "token", token,
                "password", "newpass",
                "confirmPassword", "newpass"
        );

        when(passwordResetService.isValidToken(token)).thenReturn(true);
        when(passwordResetService.getUserIdFromToken(token)).thenReturn(missingUserId);
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        // When
        Exception exception = assertThrows(Exception.class, () -> {
            authController.resetPassword(request);
        });

        // Optionally check message if needed
        assertInstanceOf(NoSuchElementException.class, exception);
    }

}