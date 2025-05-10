package com.noahalvandi.dbbserver.dto.projection;

import lombok.Data;

import java.util.List;

@Data
public class LanguagesCategories {

    List<String> categories;
    List<String> languages;
}
