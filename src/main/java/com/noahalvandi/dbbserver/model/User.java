package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    private String firstName;
    private String lastName;
    private Date dateOfBirth;

    private String phoneNumber;
    private String city;
    private String street;
    private String postalCode;

    @Column(unique = true)
    private String email;
    private String password;

    @Enumerated(EnumType.ORDINAL)
    private UserType userType;

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
