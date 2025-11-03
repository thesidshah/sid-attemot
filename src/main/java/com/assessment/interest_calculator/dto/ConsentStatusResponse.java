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
    private Long id;
    private String consentHandle;
    private String digioConsentRequestId;
    private String customerRefId;
    private String status; // PENDING, APPROVED, REJECTED, EXPIRED
    private String redirectUrl;
    private OffsetDateTime approvedAt;
    private OffsetDateTime rejectedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private String message;
}