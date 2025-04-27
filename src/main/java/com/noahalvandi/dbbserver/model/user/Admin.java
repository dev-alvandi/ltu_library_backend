package com.noahalvandi.dbbserver.model.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("0")
public class Admin extends User {
    // Optionally, you can add admin-specific fields here
}