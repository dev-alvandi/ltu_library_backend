package com.noahalvandi.dbbserver.dto.response;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class BookSuggestionsResponse {
    private List<String> title;
    private List<String> isbn;
    private List<String> author;
    private List<String> publisher;
}
