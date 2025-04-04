package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.response.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtProvider jwtProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerNewUser(@RequestBody User user) throws UserException {
        String firstName = user.getFirstName().toLowerCase();
        String lastName = user.getLastName().toLowerCase();
        Date dateOfBirth = user.getDateOfBirth();
        String phoneNumber = user.getPhoneNumber();
        String city = user.getCity().toLowerCase();
        String street = user.getStreet().toLowerCase();
        String postalCode = user.getPostalCode().toLowerCase();
        String email = user.getEmail().toLowerCase();
        String password = user.getPassword();

        User doesUserAlreadyExit = userRepository.findByEmail(email);

        if (doesUserAlreadyExit != null) {
            throw new UserException("User already exist with the entered email address.");
        }

        User createdUser = new User();
        createdUser.setFirstName(firstName);
        createdUser.setLastName(lastName);
        createdUser.setDateOfBirth(dateOfBirth);
        createdUser.setPhoneNumber(phoneNumber);
        createdUser.setCity(city);
        createdUser.setStreet(street);
        createdUser.setPostalCode(postalCode);
        createdUser.setEmail(email);
        createdUser.setPassword(password);

        User savedUser = userRepository.save(createdUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);

        AuthResponse res = new AuthResponse(token, true);

        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }
}
