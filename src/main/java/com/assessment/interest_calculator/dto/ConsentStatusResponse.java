package com.assessment.interest_calculator.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentStatusResponse {
    private String consentHandle;
    private String status; // PENDING, APPROVED, REJECTED, EXPIRED
    private String redirectUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private String message;
}