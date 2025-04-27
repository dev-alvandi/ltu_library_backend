package com.noahalvandi.dbbserver.model.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@DiscriminatorValue("2") // covers STUDENT, RESEARCHER, UNIVERSITY_STAFF
public class GeneralUser extends User {

    @Enumerated(EnumType.STRING)
    private SubUserType subUserType;

    public enum SubUserType {
        STUDENT,
        RESEARCHER,
        UNIVERSITY_STAFF
    }
}