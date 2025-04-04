package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.response.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

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

}