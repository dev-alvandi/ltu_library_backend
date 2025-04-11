package com.noahalvandi.dbbserver.repository;

import com.noahalvandi.dbbserver.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    @EntityGraph(attributePaths = {"bookCategory"})
    Page<Book> findAll(Pageable pageable);

    @Query("SELECT COUNT(bc) FROM Book b JOIN BookCopy bc ON b.bookId = bc.book.bookId WHERE b.bookId = :bookId")
    long countAllCopies(@Param("bookId") UUID bookId);

    @Query("SELECT b from Book b ")
    List<Book> findAllAvailableBooksToBorrow(@Param("bookId") UUID bookId);

    @Query("SELECT b from Book b WHERE b.author LIKE %:author%")
    public Book searchBooksByAuthor(String author);

    @Query("SELECT b from Book b WHERE b.title LIKE %:title%")
    public Book searchBooksByTitle(String title);

    @Query("SELECT b FROM Book b WHERE b.author LIKE %:author% OR b.title LIKE %:title% OR b.isbn LIKE %:isbn% OR b.publisher LIKE %:publisher%")
    public List<Book> searchBooksByAuthorOrTitleOrIsbnOrPublisher(String author, String title, String isbn, String publisher);


}
