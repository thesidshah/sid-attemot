package com.assessment.interest_calculator.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loan_accounts")
@Data //Should I use @Value?
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccount {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String accountHolderName;
    
    /**
     * Annual interest rate as a percentage (e.g., 5.5 for 5.5%)
     */
    @Column(name = "interest_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal interestRate;

    /**
     * Accumlated interest amount
     * Using NUMERIC(18,6) for higher precision in calculations.
     */
    @Column(name="interest_amount", nullable = false, precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal interestAmount = BigDecimal.ZERO;
    /**
     * Principal amount on which the interest is calculated.
     */
    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal principalAmount;

    /**
     * Date when the loan was disbursed.
     */
    @Column(name = "date_of_disbursal", nullable = false)
    private LocalDate dateOfDisbursal;
    /**
     * Timestamp of the last interest application.
     */
    @Column(name = "last_interest_applied_at")
    private OffsetDateTime lastInterestAppliedAt;
    
    


}
