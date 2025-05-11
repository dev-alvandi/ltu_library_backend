package com.noahalvandi.dbbserver.dto.response.book;

import com.noahalvandi.dbbserver.model.ItemStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class BookCopyResponse {

    private UUID bookCopyId;
    private String barcodeId; // url to the resource in S3
    private String barcodeUrl;
    private String physicalLocation;
    private ItemStatus status;
    private boolean isItemReferenceCopy;
}
