package com.assessment.interest_calculator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.LoanAccountRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;




@Slf4j
@Service
public class InterestService {
    private final LoanAccountRepository loanAccountRepository; 
    private final int dayCountBasis; // 365 or 366 for leap years
    private final ZoneId zoneId; // Time zone for date calculations

    private static final int BATCH_SIZE = 100; // Batch size for processing loan accounts
    private static final int MONEY_SCALE = 6; // Scale for monetary calculations
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP; // Rounding mode for monetary calculations

    public InterestService(
            LoanAccountRepository loanAccountRepository,
            @Value("${app.interest.dayCountBasis:365}") int dayCountBasis,
            @Value("${app.interest.zone:Asia/Kolkata}") String zone) {
        this.loanAccountRepository = loanAccountRepository;
        this.dayCountBasis = dayCountBasis;
        this.zoneId = ZoneId.of(zone);   
        log.info("InterestService initialized with dayCountBasis={} and zoneId={}", dayCountBasis, zoneId);
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

    public InterestApplicationResult applyDailyInterest(LocalDate forDate) {
        log.info("Starting daily interest application for date: {}", forDate);
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0); // Thread-safe counter for successful updates
        AtomicInteger failureCount = new AtomicInteger(0); // Thread-safe counter for failed updates
        AtomicReference<BigDecimal> totalInterestApplied = new AtomicReference<>(BigDecimal.ZERO); // Thread-safe accumulator for total interest applied

        long totalAccounts = loanAccountRepository.countAccountsNeedingInterestApplication(forDate);
        log.info("Total accounts needing interest application for {}: {}", forDate, totalAccounts);
        int totalBatches = (int) Math.ceil((double) totalAccounts / BATCH_SIZE);

        int pageNumber = 0;
        Page<LoanAccount> accountsPage;
        do {
            accountsPage = loanAccountRepository.findAccountsNeedingInterestApplication(forDate, org.springframework.data.domain.PageRequest.of(pageNumber, BATCH_SIZE));
            log.info("Processing batch {}/{} with {} accounts", pageNumber + 1, totalBatches, accountsPage.getNumberOfElements());

            for (LoanAccount account : accountsPage.getContent()) {
                try {
                    BigDecimal interestApplied = applyInterestToAccount(account, forDate);
                    successCount.incrementAndGet();
                    totalInterestApplied.updateAndGet(current -> current.add(interestApplied));
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Failed to apply interest to account {}: {}", account.getId(), e.getMessage(), e);
                }
            }

            pageNumber++;
        } while (accountsPage.hasNext());

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("Completed interest application for date: {}. Success: {}, Failures: {}, Total Interest Applied: {}, Duration: {} ms",
                forDate, successCount.get(), failureCount.get(), totalInterestApplied.get(), durationMs);

        return InterestApplicationResult.builder()
                .date(forDate)
                .totalAccountsProcessed(successCount.get() + failureCount.get())
                .failedAccounts(failureCount.get())
                .totalInterestApplied(totalInterestApplied.get())
                .durationMs(durationMs)
                .build();
    }

     @Transactional
    protected BigDecimal applyInterestToAccount(LoanAccount account, LocalDate forDate) {
        BigDecimal dailyInterest = calculateDailyInterest(
                account.getPrincipalAmount(),
                account.getInterestRate()
        );

        // Accumulate daily interest in interestAmount field (will be transferred to appliedInterest at month-end)
        BigDecimal newInterestAmount = account.getInterestAmount().add(dailyInterest);
        account.setInterestAmount(newInterestAmount);
        account.setLastInterestAppliedAt(OffsetDateTime.now(zoneId));

        loanAccountRepository.save(account);

        return dailyInterest;
    }

    public BigDecimal calculateDailyInterest(BigDecimal principalAmount, BigDecimal interestRate) {
        if (principalAmount == null || interestRate == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal rateDecimal = interestRate.divide(
                BigDecimal.valueOf(100),
                MONEY_SCALE + 2,
                ROUNDING_MODE
        );

        BigDecimal annualInterest = principalAmount.multiply(rateDecimal);

        BigDecimal dailyInterest = annualInterest.divide(
                BigDecimal.valueOf(dayCountBasis),
                MONEY_SCALE,
                ROUNDING_MODE
        );

        return dailyInterest;
    }

    /**
     * Apply accrued interest to all accounts at month-end.
     * Transfers interestAmount to principalAmount and resets interestAmount to zero.
     *
     * @param forDate The date for which to apply month-end interest (typically the last day of the month)
     * @return Result containing processing statistics
     */
    public InterestApplicationResult applyMonthEndInterest(LocalDate forDate) {
        log.info("Starting month-end interest application for date: {}", forDate);
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<BigDecimal> totalInterestApplied = new AtomicReference<>(BigDecimal.ZERO);

        long totalAccounts = loanAccountRepository.count();
        log.info("Total accounts for month-end processing: {}", totalAccounts);
        int totalBatches = (int) Math.ceil((double) totalAccounts / BATCH_SIZE);

        int pageNumber = 0;
        Page<LoanAccount> accountsPage;
        do {
            accountsPage = loanAccountRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));
            log.info("Processing month-end batch {}/{} with {} accounts", pageNumber + 1, totalBatches, accountsPage.getNumberOfElements());

            for (LoanAccount account : accountsPage.getContent()) {
                try {
                    BigDecimal interestApplied = applyAccruedInterestToPrincipal(account);
                    successCount.incrementAndGet();
                    totalInterestApplied.updateAndGet(current -> current.add(interestApplied));
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Failed to apply month-end interest to account {}: {}", account.getId(), e.getMessage(), e);
                }
            }

            pageNumber++;
        } while (accountsPage.hasNext());

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("Completed month-end interest application for date: {}. Success: {}, Failures: {}, Total Interest Applied: {}, Duration: {} ms",
                forDate, successCount.get(), failureCount.get(), totalInterestApplied.get(), durationMs);

        return InterestApplicationResult.builder()
                .date(forDate)
                .totalAccountsProcessed(successCount.get() + failureCount.get())
                .failedAccounts(failureCount.get())
                .totalInterestApplied(totalInterestApplied.get())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Apply accrued interest to the principal amount for a single account.
     * Adds interestAmount to principalAmount and resets interestAmount to zero.
     * This implements compounding by adding the accrued interest to the principal.
     *
     * @param account The account to process
     * @return The amount of interest applied to principal
     */
    @Transactional
    protected BigDecimal applyAccruedInterestToPrincipal(LoanAccount account) {
        BigDecimal accruedInterest = account.getInterestAmount();

        if (accruedInterest.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newPrincipal = account.getPrincipalAmount().add(accruedInterest);
            account.setPrincipalAmount(newPrincipal);
            account.setInterestAmount(BigDecimal.ZERO);

            loanAccountRepository.save(account);

            log.debug("Applied accrued interest {} to principal for account {}. New principal: {}",
                    accruedInterest, account.getId(), newPrincipal);
        }

        return accruedInterest;
    }

}
