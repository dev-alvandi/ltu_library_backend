package com.noahalvandi.dbbserver.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LoanResponse {
    private UUID loanId;
    private String imageUrl;
    private String title;
    private Instant borrowedAt;
    private LocalDateTime dueAt;
    private boolean isReturned;
    private LoanStatus status;
    private boolean extendable;

    public LoanResponse(UUID loanId, String imageUrl, String title,
                        Instant borrowedAt, LocalDateTime dueAt, boolean isReturned) {
        this.loanId = loanId;
        this.imageUrl = imageUrl;
        this.title = title;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.isReturned = isReturned;
    }
}

