package com.noahalvandi.dbbserver.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue()
    private UUID userId;

    @Column(length = 100, nullable = false)
    @NotBlank
    private String firstName;

    @Column(length = 100, nullable = false)
    @NotBlank
    private String lastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate dateOfBirth;


    @Column(length = 100, nullable = false)
    @Pattern(
            regexp = "^(\\+46|0)\\d{7,12}$",
            message = "Invalid Swedish phone number"
    )
    private String phoneNumber;

    @Column(length = 100, nullable = false)
    private String city;

    @Column(length = 100, nullable = false)
    private String street;

    @Column(length = 10, nullable = false)
    @Pattern(regexp = "^\\d{3}\\s?\\d{2}$", message = "Invalid Swedish postal code")
    private String postalCode;

    @Column(length = 50, nullable = false, unique = true)
    @Email
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private UserType userType;

//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Loan> loans = new ArrayList<>();
//
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Reservation> reservations = new ArrayList<>();

    @Getter
    public enum UserType {
        ADMIN(0),
        LIBRARIAN(1),
        STUDENT(2),
        RESEARCHER(3),
        UNIVERSITY_STAFF(4),
        PUBLIC(5);

        private final int code;

        UserType(int code) {
            this.code = code;
        }

        public static UserType fromCode(int code) {
            for (UserType userType : UserType.values()) {
                if (userType.getCode() == code) {
                    return userType;
                }
            }
            throw new IllegalArgumentException("Invalid code for UserType: " + code);
        }
    }
}
