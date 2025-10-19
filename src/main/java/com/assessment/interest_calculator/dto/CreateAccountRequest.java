package com.assessment.interest_calculator.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {
    @NotBlank(message = "Account holder name must not be blank")
    private String accountHolderName;

    @NotNull(message = "Principal amount must not be null")
    @DecimalMin(value = "0.01", message = "Principal amount must be greater than 0")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate must not be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Interest rate must be >= 0")
    @DecimalMax(value = "100.0", message = "Interest rate must be <= 100")
    private BigDecimal interestRate;

    @NotNull(message = "Date of disbursal must not be null")
    private LocalDate dateOfDisbursal;
}
