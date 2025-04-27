package com.noahalvandi.dbbserver.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LanguageBookCount {

    private String language;
    private long count;
}
