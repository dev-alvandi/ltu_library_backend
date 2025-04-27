package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Book {

    @Id
    @GeneratedValue()
    private UUID bookId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false,length = 20, unique = true)
    private String isbn;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false, length = 100)
    private String publisher;

    @Min(1000)
    @Max(value = Year.MAX_VALUE, message = "Publishing year cannot be in the future")
    private int publishedYear;

    @Column(nullable = false, length = 100)
    private String language;

    @Column(length = 1000, unique = true)
    private String image_url;

    @ManyToOne()
    @JoinColumn(name = "book_category_id")
    private BookCategory bookCategory;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private BookType bookType;

//    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<BookCopy> bookCopies = new ArrayList<>();
//
//    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Reservation> reservations = new ArrayList<>();

    @Getter
    public enum BookType {
        COURSE_LITERATURE(0),
        PUBLIC(1);

        private final int code;

        BookType(int code) {
            this.code = code;
        }

        public static BookType fromCode(int code) {
            for (BookType bookType : BookType.values()) {
                if (bookType.getCode() == code) {
                    return bookType;
                }
            }
            throw new IllegalArgumentException("Invalid code for BookType: " + code);
        }
    }
}
