package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class BookCategory {

    @Id
    @GeneratedValue()
    private UUID bookCategoryId;

    @Column(length = 50, nullable = false, unique = true)
    private String subject;

//    @OneToMany(mappedBy = "bookCategory")
//    private List<Book> books = new ArrayList<>();
}
