package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.security.JwtProvider;
import com.noahalvandi.dbbserver.dto.projection.AuthResponse;
import com.noahalvandi.dbbserver.dto.projection.PasswordRequest;
import com.noahalvandi.dbbserver.dto.request.UserRequest;
import com.noahalvandi.dbbserver.dto.response.LoanResponse;
import com.noahalvandi.dbbserver.dto.response.ReservationResponse;
import com.noahalvandi.dbbserver.dto.response.UserResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.UserResponseMapper;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.BookCopy;
import com.noahalvandi.dbbserver.model.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import com.noahalvandi.dbbserver.service.ReservationService;
import com.noahalvandi.dbbserver.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final ReservationService reservationService;


    @PutMapping("/update-profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @Valid @RequestBody UserRequest request,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {
        User foundUser = userService.findUserProfileByJwt(jwt);
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        if (!foundUser.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.findByEmail(request.getEmail().toLowerCase()) != null) {
            throw new UserException("Email already in use.");
        }

        foundUser.setFirstName(request.getFirstName().toLowerCase());
        foundUser.setLastName(request.getLastName().toLowerCase());
        foundUser.setEmail(request.getEmail().toLowerCase());
        foundUser.setPhoneNumber(request.getPhoneNumber().toLowerCase());
        foundUser.setCity(request.getCity().toLowerCase());
        foundUser.setStreet(request.getStreet().toLowerCase());
        foundUser.setPostalCode(request.getPostalCode().toLowerCase());

        User updatedUser = userRepository.save(foundUser);

        // Manually rebuild Authentication
        Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                updatedUser.getEmail(),
                null,
                currentAuth.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);

        // Generate new JWT
        String newToken = jwtProvider.generateToken(newAuthentication);
        UserResponse userResponse = UserResponseMapper.toDto(updatedUser);

        return new ResponseEntity<>(new AuthResponse(newToken, userResponse, true), HttpStatus.OK);
    }


    @PutMapping("/update-password")
    public ResponseEntity<UserResponse> updatePassword(
            @Valid @RequestBody PasswordRequest request,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);


        // 2. Validate old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UserException("Old password is incorrect.");
        }

        // 3. Check if new password and confirm match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new UserException("New passwords do not match.");
        }

        // 4. Update password (encode it!)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User updatedUser = userRepository.save(user);

        UserResponse userResponse = UserResponseMapper.toDto(updatedUser);

        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }


    @PostMapping("/borrow/{bookId}")
    public ResponseEntity<BookCopy> borrowBookCopy(
            @PathVariable UUID bookId,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        BookCopy borrowedCopy = userService.borrowBookCopy(user ,bookId);

        return new ResponseEntity<>(borrowedCopy, HttpStatus.CREATED) ;
    }

    @GetMapping("/loans")
    public ResponseEntity<Page<LoanResponse>> getUserLoan(
            @RequestHeader("Authorization") String jwt,
            @PageableDefault(page = 0, size = 10, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        Page<LoanResponse> loan = userService.getUserLoan(user, pageable);

        return new ResponseEntity<>(loan, HttpStatus.OK);
    }

    @PostMapping("/reserve/{bookId}")
    public ResponseEntity<ReservationResponse> reserveBookCopy(
            @PathVariable UUID bookId,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        ReservationResponse response = reservationService.reserveBookCopy(user, bookId);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/cancel-reservation/{reservationId}")
    public ResponseEntity<String> cancelReservation(
            @PathVariable UUID reservationId,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {
        User user = userService.findUserProfileByJwt(jwt);

        reservationService.deleteReservation(user.getUserId(), reservationId);

        return new ResponseEntity<>("Reservation cancelled successfully.", HttpStatus.OK);
    }

    @GetMapping("/reservations")
    public ResponseEntity<Page<ReservationResponse>> getUserReservations(
            @RequestHeader("Authorization") String jwt,
            @PageableDefault(page = 0, size = 10, sort = "reservedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        Page<ReservationResponse> reservationResponses =  reservationService.getUserReservations(user.getUserId(), pageable);

        return new ResponseEntity<>(reservationResponses, HttpStatus.OK);
    }

    @PutMapping("/return-resource/{barcode}")
    public ResponseEntity<String> returnResource(
            @PathVariable String barcode,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        if (!userService.isAdminOrLibrarian(user)) {
            throw new UserException("User is not admin or librarian.");
        }

        String response = userService.returnResource(barcode);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/extend-loan/{loanId}")
    public ResponseEntity<String> returnResource(
            @PathVariable UUID loanId,
            @RequestHeader("Authorization") String jwt
    ) throws UserException {

        User user = userService.findUserProfileByJwt(jwt);

        String response = userService.extendLoan(loanId);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
