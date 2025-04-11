package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.response.BookDto;
import com.noahalvandi.dbbserver.dto.response.mapper.BookDtoMapper;
import com.noahalvandi.dbbserver.model.Book;
import com.noahalvandi.dbbserver.repository.BookCopyRepository;
import com.noahalvandi.dbbserver.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;



@Service
public class ResourceServiceImplementation implements ResourceService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Override
    public Page<BookDto> getAllBooks(Pageable pageable) {

        Page<Book> books = bookRepository.findAll(pageable);

        Page<BookDto> pageBookDtos = books.map(BookDtoMapper::toDto);

        pageBookDtos.getContent().forEach(bookDto -> {
            bookDto.setNumberOfCopies(bookCopyRepository.countAllNonReferenceCopiesByBookId(bookDto.getBookId()));
            bookDto.setNumberOfAvailableToBorrowCopies(bookCopyRepository.numberOfAvailableBookCopiesToBorrow(bookDto.getBookId()));
        });

        return pageBookDtos;
    }
}
