package com.noahalvandi.dbbserver.dto.response.mapper;

import com.noahalvandi.dbbserver.dto.response.BookResponse;
import com.noahalvandi.dbbserver.model.Book;


public class BookResponseMapper {


    public static BookResponse toDto(Book book) {

        BookResponse dto = new BookResponse();

        dto.setBookId(book.getBookId());
        dto.setTitle(book.getTitle());
        dto.setIsbn(book.getIsbn());
        dto.setAuthor(book.getAuthor());
        dto.setPublisher(book.getPublisher());
        dto.setPublishedYear(book.getPublishedYear());
        dto.setLanguage(book.getLanguage());
        dto.setImageUrl(book.getImageUrl());
        dto.setBookCategory(book.getBookCategory());
        dto.setBookType(book.getBookType());


        return dto;

    }
}
