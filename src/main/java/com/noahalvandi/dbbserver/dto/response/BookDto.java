package com.noahalvandi.dbbserver.dto.response;

import com.noahalvandi.dbbserver.model.Book;
import com.noahalvandi.dbbserver.model.BookCategory;
import lombok.Data;

import java.util.UUID;

@Data
public class BookDto {

    private UUID bookId;
    private String title;
    private String isbn;
    private String author;
    private String publisher;
    private int publishedYear;
    private String language;
    private String imageUrl;
    private BookCategory bookCategory;
    private Book.BookType bookType;
}
