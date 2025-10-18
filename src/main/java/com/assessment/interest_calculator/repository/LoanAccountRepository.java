package com.assessment.interest_calculator.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.assessment.interest_calculator.entity.LoanAccount;

/**
 * Repository for LoanAccount entity operations.
 * Includes methods for batch processing and concurrency-safe updates.
 */
@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    /**
     * Find all accounts that need interest application for a given date.
     * This method should support pagination for batch processing.
     * 
     * @param forDate The date for which to find accounts needing interest application.
     * @param pageable The pagination information.
     * @return A page of loan accounts needing interest application.
     */
    @Query("SELECT la FROM LoanAccount la WHERE " +
           "(la.lastInterestAppliedDate IS NULL"+ 
           " OR la.lastInterestAppliedDate < :forDate)")
           Page<LoanAccount> findAccountsNeedingInterestApplication(@Param("forDate")LocalDate forDate, Pageable pageable);
}
