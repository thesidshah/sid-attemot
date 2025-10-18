package com.assessment.interest_calculator.service;

import java.time.ZoneId;

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
    

    
}
