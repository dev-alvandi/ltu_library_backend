package com.noahalvandi.dbbserver.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

@Entity
@Data
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookId;

    private String title;
    private String isbn;
    private String author;
    private String publisher;
    private int publishingYear;
    private String imageURL;

    @ManyToOne
    @JoinColumn(name = "bookCategoryId")
    private BookCategory bookCategory;

    @Enumerated(EnumType.ORDINAL)
    private BookType bookType;

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
