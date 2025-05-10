package com.noahalvandi.dbbserver.dto.projection.book;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookCategoryCount {

    private String subject;
    private long count;
}
