package com.noahalvandi.dbbserver.dto.request;

import com.noahalvandi.dbbserver.model.Book;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BookRequest {

    private UUID bookId;
    private String title;
    private String author;
    private String publisher;
    private String publishedYear;
    private String isbn;
    private String language;
    private Book.BookType bookType;
    private String bookCategory;

    public BookRequest(String title, String author, String publisher, String publishedYear, String isbn, String language, Book.BookType bookType, String bookCategory) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.isbn = isbn;
        this.language = language;
        this.bookType = bookType;
        this.bookCategory = bookCategory;
    }
}
