package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.response.AuthResponse;
import com.noahalvandi.dbbserver.service.CustomUserDetailsServiceImplementation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtProvider jwtProvider;
    private CustomUserDetailsServiceImplementation customUserDetails;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider, CustomUserDetailsServiceImplementation customUserDetails) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.customUserDetails = customUserDetails;
    }

    @GetMapping("/debug/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerNewUser(@RequestBody User user) throws UserException {

        String firstName = user.getFirstName().toLowerCase();
        String lastName = user.getLastName().toLowerCase();
        LocalDate dateOfBirth = user.getDateOfBirth();
        String phoneNumber = user.getPhoneNumber();
        String city = user.getCity().toLowerCase();
        String street = user.getStreet().toLowerCase();
        String postalCode = user.getPostalCode().toLowerCase();
        String email = user.getEmail().toLowerCase();
        String password = user.getPassword();

        System.out.println(dateOfBirth);

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
        createdUser.setPassword(passwordEncoder.encode(password));

        User savedUser = userRepository.save(createdUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);

        AuthResponse res = new AuthResponse(token, true);

        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody User user) throws UserException {

        String email = user.getEmail().toLowerCase();
        String password = user.getPassword();

        Authentication authentication = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);
        System.out.println(token);

        AuthResponse res = new AuthResponse(token, true);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    private Authentication authenticate(String username, String password) {
        UserDetails userDetails = customUserDetails.loadUserByUsername(username);

        if (userDetails == null) {
            throw new BadCredentialsException("No user with the entered credentials found");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Username or password invalid");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
