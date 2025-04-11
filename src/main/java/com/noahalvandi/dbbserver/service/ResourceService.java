package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.response.BookDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceService {

    public Page<BookDto> getAllBooks(Pageable pageable);
}
