package com.noahalvandi.dbbserver.dto.response.film;

import com.noahalvandi.dbbserver.model.ItemStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class FilmCopyResponse {

    private UUID filmCopyId;
    private String barcodeId;
    private String barcodeUrl;
    private String physicalLocation;
    private ItemStatus status;
    private boolean isItemReferenceCopy;
}
