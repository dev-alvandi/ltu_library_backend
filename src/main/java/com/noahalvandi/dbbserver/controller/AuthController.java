package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.response.AuthResponse;
import com.noahalvandi.dbbserver.service.CustomUserDetailsServiceImplementation;
import com.noahalvandi.dbbserver.service.PasswordResetService;
import com.noahalvandi.dbbserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CustomUserDetailsServiceImplementation customUserDetails;

    @Autowired
    private PasswordResetService passwordResetService;
    @Autowired
    private UserService userService;

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
        createdUser.setUserType(userService.determineUserTypeByEmail(email, firstName, lastName));

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

    @PostMapping("/request-password-reset")
    public ResponseEntity<String> requestPasswordReset(@RequestBody Map<String, String> request) {
        System.out.println(request);

        String email = request.get("email");
        passwordResetService.sendPasswordResetToken(email);
        return ResponseEntity.ok("Reset link sent if email exists.");
    }

    @PostMapping("/password-reset")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {

        String token = request.get("token");
        String newPassword = request.get("password");
        String confirmNewPassword = request.get("confirmPassword");

        if (!passwordResetService.isValidToken(token)) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            return ResponseEntity.badRequest().body("Password and confirm password do not match");
        }

        Integer userId = passwordResetService.getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // Hash password and update
        User user = userRepository.findById(userId).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetService.invalidateToken(token);
        return ResponseEntity.ok("Password updated successfully");
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
