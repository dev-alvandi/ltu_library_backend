package com.noahalvandi.dbbserver.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * The {@code JwtProvider} class is responsible for generating and validating JWT (JSON Web Tokens).
 * It utilizes the HMAC SHA key for signing tokens and extracting user information such as email from tokens.
 */
@Service
public class JwtProvider {
    SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

    /**
     * Generates a JWT for the provided authentication object.
     *
     * @param auth the {@link Authentication} containing user details
     * @return a compact, URL-safe JWT as a {@link String}
     */
    public String generateToken(Authentication auth) {
        int DAY_TO_MILLISECONDS = 1000 * 60 * 60 * 24;

        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + DAY_TO_MILLISECONDS))
                .claim("email", auth.getName())
                .signWith(key)
                .compact();
    }

    /**
     * Extracts the email claim from the given JWT.
     *
     * @param jwt the JWT from which to extract the email claim
     * @return the email as a {@link String}
     */
    public String getEmailFromToken(String jwt) {
        jwt = jwt.substring(7);

        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();

        return String.valueOf(claims.get("email"));
    }
}
