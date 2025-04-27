package com.noahalvandi.dbbserver.model.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("1")
public class Librarian extends User {

    private LocalDate employmentDate;

    private String employeeNumber; // optional, if you want internal IDs
}