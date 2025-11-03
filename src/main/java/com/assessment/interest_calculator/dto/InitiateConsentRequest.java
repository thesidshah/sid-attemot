package com.assessment.interest_calculator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiateConsentRequest {
    
    @NotNull(message = "Loan account ID is required")
    private Long loanAccountId;
    
    @NotBlank(message = "Customer mobile is required")
    private String customerMobile;
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    
    private String customerIdentifier;
    
    private String pan;
    
    private String dob;
}
