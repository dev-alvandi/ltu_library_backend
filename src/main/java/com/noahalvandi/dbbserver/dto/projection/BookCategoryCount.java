package com.noahalvandi.dbbserver.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookCategoryCount {

    private String subject;
    private long count;
}
