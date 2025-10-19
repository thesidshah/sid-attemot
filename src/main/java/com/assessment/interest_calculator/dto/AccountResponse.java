package com.assessment.interest_calculator.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
private Long id;
private String name;
private BigDecimal interestRate;
private BigDecimal interestAmount;
private BigDecimal principalAmount;
private LocalDate dateOfDisbursal;
private OffsetDateTime lastInterestAppliedAt;
private Long version;
private OffsetDateTime createdAt;
private OffsetDateTime updatedAt;    
}
