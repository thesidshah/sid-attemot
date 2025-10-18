package com.assessment.interest_calculator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.assessment.interest_calculator.repository.LoanAccountRepository;

import lombok.extern.slf4j.Slf4j;




@Slf4j
@Service
public class InterestService {
    private final LoanAccountRepository loanAccountRepository; 
    private final int dayCountBasis; // 365 or 366 for leap years
    private final ZoneId zoneId; // Time zone for date calculations

    private static final int BATCH_SIZE = 100; // Batch size for processing loan accounts
    private static final int MONEY_SCALE = 6; // Scale for monetary calculations
    private static final RoundingMode MONEY_ROUNDING_MODE = RoundingMode.HALF_UP; // Rounding mode for monetary calculations

    public InterestService(
            LoanAccountRepository loanAccountRepository,
            @Value("${app.interest.dayCountBasis:365}") int dayCountBasis,
            @Value("${app.interest.zone:Asia/Kolkata}") String zone) {
        this.loanAccountRepository = loanAccountRepository;
        this.dayCountBasis = dayCountBasis;
        this.zoneId = ZoneId.of(zone);   
            }
        
    
    @lombok.Data
    @lombok.Builder
    static class InterestApplicationResult {
        private LocalDate date;
        private int totalAccountsProcessed;
        private int failedAccounts;
        private BigDecimal totalInterestApplied; // Sum of interest applied across all accounts - suggested by copilot
        private long durationMs;
    }
}
