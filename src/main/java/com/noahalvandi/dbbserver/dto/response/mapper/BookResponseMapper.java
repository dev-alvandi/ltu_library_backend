package com.noahalvandi.dbbserver.dto.response.mapper;

import com.noahalvandi.dbbserver.dto.response.BookDto;
import com.noahalvandi.dbbserver.model.Book;
import com.noahalvandi.dbbserver.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;


public class BookDtoMapper {


    public static BookDto toDto(Book book) {

        BookDto dto = new BookDto();

        dto.setBookId(book.getBookId());
        dto.setTitle(book.getTitle());
        dto.setIsbn(book.getIsbn());
        dto.setAuthor(book.getAuthor());
        dto.setPublisher(book.getPublisher());
        dto.setPublishedYear(book.getPublishedYear());
        dto.setLanguage(book.getLanguage());
        dto.setImageUrl(book.getImage_url());
        dto.setBookCategory(book.getBookCategory());
        dto.setBookType(book.getBookType());


        return dto;

    }
}
