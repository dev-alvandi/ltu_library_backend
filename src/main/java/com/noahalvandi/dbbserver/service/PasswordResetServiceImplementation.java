package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import com.noahalvandi.dbbserver.util.PasswordResetToken;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class PasswordResetServiceImplementation implements PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();
    @Autowired
    private GlobalConstants globalConstants;


    @Override
    public void sendPasswordResetToken(String email) {
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByEmail(email));
        if (userOpt.isPresent()) {
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(GlobalConstants.MINUTES_TO_EXPIRE_PASSWORD_TOKEN);
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

        String htmlContent = getHtmlTemplate(resetLink);
        helper.setText(htmlContent, true); // true = isHtml

        mailSender.send(mimeMessage);
    }

    private String getHtmlTemplate(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
                <head>
                  <style>
                    .container {
                      font-family: Arial, sans-serif;
                      background-color: #f9f9f9;
                      padding: 30px;
                      border-radius: 10px;
                      max-width: 500px;
                      margin: auto;
                      box-shadow: 0 0 10px rgba(0,0,0,0.1);
                    }
                    .btn {
                      display: inline-block;
                      padding: 10px 20px;
                      margin-top: 20px;
                      background-color: #0F427E;
                      color: white;
                      text-decoration: none;
                      border-radius: 5px;
                    }
                    .footer {
                      font-size: 12px;
                      color: #999;
                      margin-top: 30px;
                    }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <h2>Password Reset Request</h2>
                    <p>You requested to reset your password. Click the button below to continue:</p>
                    <a href="%s" class="btn">Reset Password</a>
                    <p>If you didn't request this, you can ignore this email.</p>
                    <div class="footer">This link will expire in %d minutes.</div>
                  </div>
                </body>
            </html>
        """.formatted(resetLink, GlobalConstants.MINUTES_TO_EXPIRE_PASSWORD_TOKEN);
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
