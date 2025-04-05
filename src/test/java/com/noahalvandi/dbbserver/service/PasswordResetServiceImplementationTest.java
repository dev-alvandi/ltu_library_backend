package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import com.noahalvandi.dbbserver.util.PasswordResetToken;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceImplementationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @Spy
    private GlobalConstants globalConstants = new GlobalConstants(); // or mock if needed

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private PasswordResetServiceImplementation passwordResetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendPasswordResetToken_shouldGenerateAndSendEmail() throws Exception {
        // Given
        String email = "test@example.com";
        User user = new User();
        user.setUserId(1);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        passwordResetService.sendPasswordResetToken(email);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
        // Optionally, check internal state
        assertTrue(passwordResetService.isValidTokenInMemory());
    }

    @Test
    void sendPasswordResetToken_shouldNotFailIfEmailNotFound() {
        // Given
        String email = "notfound@example.com";
        when(userRepository.findByEmail(email)).thenReturn(null);

        // When
        assertDoesNotThrow(() -> passwordResetService.sendPasswordResetToken(email));

        // Then
        verify(mailSender, never()).send((MimeMessage) any());
    }

    @Test
    void isValidToken_shouldReturnTrueIfNotExpired() {
        // Given
        String token = "token123";
        int userId = 1;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        PasswordResetToken tokenObj = new PasswordResetToken(userId, expiresAt);

        passwordResetService.getResetTokensMap().put(token, tokenObj);

        // When
        boolean result = passwordResetService.isValidToken(token);

        // Then
        assertTrue(result);
    }

    @Test
    void isValidToken_shouldReturnFalseIfExpired() {
        // Given
        String token = "token123";
        int userId = 1;
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(1);
        PasswordResetToken tokenObj = new PasswordResetToken(userId, expiredTime);

        passwordResetService.getResetTokensMap().put(token, tokenObj);

        // When
        boolean result = passwordResetService.isValidToken(token);

        // Then
        assertFalse(result);
    }

    @Test
    void getUserIdFromToken_shouldReturnUserIdIfValid() {
        String token = "valid-token";
        int userId = 42;
        passwordResetService.getResetTokensMap().put(token, new PasswordResetToken(userId, LocalDateTime.now().plusMinutes(5)));

        Integer result = passwordResetService.getUserIdFromToken(token);

        assertEquals(userId, result);
    }

    @Test
    void getUserIdFromToken_shouldReturnNullIfInvalid() {
        String token = "missing-token";
        assertNull(passwordResetService.getUserIdFromToken(token));
    }

    @Test
    void invalidateToken_shouldRemoveToken() {
        String token = "delete-me";
        passwordResetService.getResetTokensMap().put(token, new PasswordResetToken(1, LocalDateTime.now().plusMinutes(5)));

        passwordResetService.invalidateToken(token);

        assertFalse(passwordResetService.getResetTokensMap().containsKey(token));
    }

    @Test
    void sendStyledPasswordResetEmail_shouldSendMimeMessage() throws Exception {
        // Given
        String email = "test@example.com";
        String link = "http://reset-link.com";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        passwordResetService.sendStyledPasswordResetEmail(email, link);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }
}