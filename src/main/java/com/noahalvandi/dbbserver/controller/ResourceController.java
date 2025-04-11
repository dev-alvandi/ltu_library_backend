package com.noahalvandi.dbbserver.controller;

import com.noahalvandi.dbbserver.dto.response.BookDto;
import com.noahalvandi.dbbserver.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @GetMapping("/books")
    public ResponseEntity<Page<BookDto>> getAllBooks(@PageableDefault(page = 0, size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BookDto> books = resourceService.getAllBooks(pageable);
        return new ResponseEntity<>(books, HttpStatus.OK);
    }
}
