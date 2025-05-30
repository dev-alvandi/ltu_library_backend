package com.noahalvandi.dbbserver.dto.projection.book;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@ToString
public class BookFilterCriteria {

    private boolean isAvailable;
    private Integer minYear;
    private Integer maxYear;
    private List<String> categories;
    private List<String> languages;
}
