package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.util.EmailTemplates;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import com.noahalvandi.dbbserver.util.PasswordResetToken;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImplementation implements PasswordResetService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();


    @Override
    public void sendPasswordResetToken(String email) {
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByEmail(email));
        if (userOpt.isPresent()) {
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(GlobalConstants.MINUTES_TO_EXPIRE_RESET_PASSWORD_TOKEN);
            resetTokens.put(token, new PasswordResetToken(userOpt.get().getUserId(), expiresAt));

            String resetLink = GlobalConstants.FRONTEND_BASE_URL + "/auth/password-reset/" + token;

            try {
                sendStyledPasswordResetEmail(email, resetLink);
            } catch (MessagingException e) {
                // Log properly in real apps
                log.error("Failed to send the email to the address: {}: {}", email, e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isValidToken(String token) {
        PasswordResetToken prt = resetTokens.get(token);
        return prt != null && prt.getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        PasswordResetToken prt = resetTokens.get(token);
        return prt != null ? prt.getUserId() : null;
    }

    @Override
    public void invalidateToken(String token) {
        resetTokens.remove(token);
    }

    @Override
    public void sendStyledPasswordResetEmail(String toEmail, String resetLink) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Reset Your Password");

        String htmlContent = EmailTemplates.getResetPasswordTemplate(resetLink);
        helper.setText(htmlContent, true); // true = isHtml

        mailSender.send(mimeMessage);
    }

    // Only for test access, you can protect these if needed
    public Map<String, PasswordResetToken> getResetTokensMap() {
        return resetTokens;
    }

    // check presence in tests
    public boolean isValidTokenInMemory() {
        return !resetTokens.isEmpty();
    }
}
