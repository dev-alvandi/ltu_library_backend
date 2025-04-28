package com.noahalvandi.dbbserver.dto.request;

import lombok.Data;

@Data
public class BookCopyRequest {

    private String status;
    private boolean itemReferenceCopy;
    private String physicalLocation;

}
