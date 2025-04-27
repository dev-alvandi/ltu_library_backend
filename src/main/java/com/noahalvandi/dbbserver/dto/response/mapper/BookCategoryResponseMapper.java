package com.noahalvandi.dbbserver.dto.response.mapper;

import com.noahalvandi.dbbserver.dto.response.BookCategoryResponse;
import com.noahalvandi.dbbserver.model.BookCategory;

public class BookCategoryResponseMapper {

    public static BookCategoryResponse toDto(BookCategory bookCategory) {

        BookCategoryResponse bookCategoryResponse = new BookCategoryResponse();
        bookCategoryResponse.setCategoryName(bookCategory.getSubject());

        return bookCategoryResponse;
    }
}
