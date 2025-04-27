package com.noahalvandi.dbbserver.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

/**
 * The {@code JwtTokenValidator} class is a filter that processes incoming HTTP requests to validate JSON Web Tokens (JWTs).
 * It extracts user information from a valid token and sets the authentication in the security context.
 */
public class JwtTokenValidator extends OncePerRequestFilter {

    private int HOUR_TO_MILLISECONDS = 3_600_000;

    /**
     * Validates the provided JWT in the HTTP request header and sets the authentication in the security context.
     *
     * @param request the {@link HttpServletRequest} containing the JWT in the header
     * @param response the {@link HttpServletResponse} used for the response
     * @param filterChain the {@link FilterChain} to continue the filter processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs during filter processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        if (jwt != null) {
            jwt = jwt.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
                Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();

                String email = String.valueOf(claims.get("email"));
                String authorities = String.valueOf(claims.get("authorities"));

                List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
                Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, auths);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid token: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
