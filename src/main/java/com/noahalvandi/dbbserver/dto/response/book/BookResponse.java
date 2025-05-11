package com.noahalvandi.dbbserver.dto.response.book;

import com.noahalvandi.dbbserver.dto.HasImageUrl;
import com.noahalvandi.dbbserver.model.Book;
import com.noahalvandi.dbbserver.model.BookCategory;
import lombok.Data;

import java.util.UUID;

@Data
public class BookResponse implements HasImageUrl {

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

//    Derived attributes
    private int numberOfCopies;
    private int numberOfAvailableToBorrowCopies;
}
