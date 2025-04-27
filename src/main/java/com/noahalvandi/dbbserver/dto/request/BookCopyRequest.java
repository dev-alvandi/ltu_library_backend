package com.noahalvandi.dbbserver.dto.request;

import lombok.Data;

@Data
public class BookCopyRequest {

    private boolean itemReferenceCopy;
    private String physicalLocation;

}
