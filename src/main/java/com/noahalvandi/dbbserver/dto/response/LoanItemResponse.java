package com.noahalvandi.dbbserver.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LoanItemResponse {

    private UUID loanItemId;
    private String imageUrl;
    private String title;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
    private boolean isReturned;
    private LoanStatus status;

    public LoanItemResponse(UUID loanItemId, String imageUrl, String title, LocalDateTime borrowedAt, LocalDateTime dueAt, boolean isReturned) {
        this.loanItemId = loanItemId;
        this.imageUrl = imageUrl;
        this.title = title;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.isReturned = isReturned;
    }
}

