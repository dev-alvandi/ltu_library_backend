package com.noahalvandi.dbbserver.dto.request.mapper;

import com.noahalvandi.dbbserver.dto.request.BookRequest;
import com.noahalvandi.dbbserver.model.Book;

public class BookRequestMapper {
    public static Book toEntity(BookRequest bookRequest) {

        Book book = new Book();

        book.setTitle(bookRequest.getTitle());
        book.setAuthor(bookRequest.getAuthor());
        book.setPublisher(bookRequest.getPublisher());
        book.setPublishedYear(Integer.parseInt(bookRequest.getPublishedYear()));
        book.setIsbn(bookRequest.getIsbn());
        book.setLanguage(bookRequest.getLanguage());
        book.setBookType(bookRequest.getBookType());

        return book;
        
    }
}
