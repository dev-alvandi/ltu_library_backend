package com.noahalvandi.dbbserver.dto.request.film;

import lombok.Data;

@Data
public class FilmCopyRequest {

    private String status;
    private boolean itemReferenceCopy;
    private String physicalLocation;

}
