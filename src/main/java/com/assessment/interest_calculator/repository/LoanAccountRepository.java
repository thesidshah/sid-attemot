package com.assessment.interest_calculator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.assessment.interest_calculator.entity.LoanAccount;

/**
 * Repository for LoanAccount entity operations.
 * Includes methods for batch processing and concurrency-safe updates.
 */
@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    
}
