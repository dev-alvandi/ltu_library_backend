package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Integer> {

    @Query("SELECT b from Book b WHERE b.author LIKE %:author%")
    public Book searchBooksByAuthor(String author);

    @Query("SELECT b from Book b WHERE b.title LIKE %:title%")
    public com.noahalvandi.dbbserver.model.Book searchBooksByTitle(String title);

    @Query("SELECT b FROM Book b WHERE b.author LIKE %:author% OR b.title LIKE %:title% OR b.isbn LIKE %:isbn% OR b.publisher LIKE %:publisher%")
    public List<Book> searchBooksByAuthorOrTitleOrIsbnOrPublisher(String author, String title, String isbn, String publisher);
}
