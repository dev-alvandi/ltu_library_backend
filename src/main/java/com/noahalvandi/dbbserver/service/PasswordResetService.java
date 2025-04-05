package com.noahalvandi.dbbserver.service;

import jakarta.mail.MessagingException;

public interface PasswordResetService {

    public void sendPasswordResetToken(String email);

    public boolean isValidToken(String token);

    public Integer getUserIdFromToken(String token);

    public void invalidateToken(String token);

    public void sendStyledPasswordResetEmail(String toEmail, String resetLink) throws MessagingException;


}
