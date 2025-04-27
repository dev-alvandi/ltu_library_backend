package com.noahalvandi.dbbserver.dto.response.mapper;

import com.noahalvandi.dbbserver.dto.response.BookCopyResponse;
import com.noahalvandi.dbbserver.model.BookCopy;

public class BookCopyResponseMapper {

    public static BookCopyResponse toDto(BookCopy bookCopy) {

        BookCopyResponse bookCopyResponse = new BookCopyResponse();

        bookCopyResponse.setBookCopyId(bookCopy.getBookCopyId());
        bookCopyResponse.setBarcodeId(bookCopy.getBarcode());
        bookCopyResponse.setPhysicalLocation(bookCopy.getPhysicalLocation());
        bookCopyResponse.setStatus(bookCopy.getStatus());
        bookCopyResponse.setItemReferenceCopy(bookCopy.getIsReferenceCopy().isValue());

        return bookCopyResponse;
    }
}
