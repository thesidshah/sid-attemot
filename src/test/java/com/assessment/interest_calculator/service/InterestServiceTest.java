package com.assessment.interest_calculator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.LoanAccountRepository;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private LoanAccountRepository loanAccountRepository;

    private InterestService interestService;

    private static final int DAY_COUNT_BASIS = 365;
    private static final String ZONE_ID = "Asia/Kolkata";

    @BeforeEach
    void setUp() {
        interestService = new InterestService(loanAccountRepository, DAY_COUNT_BASIS, ZONE_ID);
    }

    @Test
    void testCalculateDailyInterest_BasicCalculation() {
        // Given: Principal of 100,000 at 10% annual interest
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = new BigDecimal("10.00");

        // When
        BigDecimal dailyInterest = interestService.calculateDailyInterest(principal, annualRate);

        // Then: Daily interest = (100000 * 0.10) / 365 = 27.397260
        BigDecimal expected = new BigDecimal("27.397260");
        assertEquals(expected, dailyInterest);
    }

    @Test
    void testCalculateDailyInterest_WithNullPrincipal() {
        // Given
        BigDecimal principal = null;
        BigDecimal annualRate = new BigDecimal("10.00");

        // When
        BigDecimal dailyInterest = interestService.calculateDailyInterest(principal, annualRate);

        // Then
        assertEquals(BigDecimal.ZERO, dailyInterest);
    }

    @Test
    void testCalculateDailyInterest_WithNullRate() {
        // Given
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = null;

        // When
        BigDecimal dailyInterest = interestService.calculateDailyInterest(principal, annualRate);

        // Then
        assertEquals(BigDecimal.ZERO, dailyInterest);
    }

    @Test
    void testCalculateDailyInterest_PrecisionTest() {
        // Given: Test with odd numbers to verify precision
        BigDecimal principal = new BigDecimal("123456.78");
        BigDecimal annualRate = new BigDecimal("7.25");

        // When
        BigDecimal dailyInterest = interestService.calculateDailyInterest(principal, annualRate);

        // Then: Daily interest = (123456.78 * 0.0725) / 365 = 24.522237
        BigDecimal expected = new BigDecimal("24.522237");
        assertEquals(expected, dailyInterest);
    }

    @Test
    void testApplyInterestToAccount_AccumulatesInInterestAmount() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        LoanAccount account = LoanAccount.builder()
                .id(1L)
                .accountHolderName("Test User")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("10.00"))
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2024, 1, 1))
                .build();

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(account);

        // When
        BigDecimal appliedInterest = interestService.applyInterestToAccount(account, testDate);

        // Then
        assertEquals(new BigDecimal("27.397260"), appliedInterest);

        ArgumentCaptor<LoanAccount> accountCaptor = ArgumentCaptor.forClass(LoanAccount.class);
        verify(loanAccountRepository).save(accountCaptor.capture());

        LoanAccount savedAccount = accountCaptor.getValue();
        assertEquals(new BigDecimal("27.397260"), savedAccount.getInterestAmount());
        assertEquals(new BigDecimal("100000.00"), savedAccount.getPrincipalAmount()); // Principal unchanged
        assertNotNull(savedAccount.getLastInterestAppliedAt());
    }

    @Test
    void testApplyInterestToAccount_AccumulatesMultipleDays() {
        // Given: Account already has some accrued interest
        LocalDate testDate = LocalDate.of(2024, 1, 16);
        LoanAccount account = LoanAccount.builder()
                .id(1L)
                .accountHolderName("Test User")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("10.00"))
                .interestAmount(new BigDecimal("27.397260")) // Already 1 day of interest
                .dateOfDisbursal(LocalDate.of(2024, 1, 1))
                .build();

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(account);

        // When: Apply second day's interest
        interestService.applyInterestToAccount(account, testDate);

        // Then
        ArgumentCaptor<LoanAccount> accountCaptor = ArgumentCaptor.forClass(LoanAccount.class);
        verify(loanAccountRepository).save(accountCaptor.capture());

        LoanAccount savedAccount = accountCaptor.getValue();
        // Should have 2 days of interest accumulated
        assertEquals(new BigDecimal("54.794520"), savedAccount.getInterestAmount());
        assertEquals(new BigDecimal("100000.00"), savedAccount.getPrincipalAmount()); // Principal still unchanged
    }

    @Test
    void testApplyAccruedInterestToPrincipal_TransfersInterestToPrincipal() {
        // Given: Account with accrued interest after 30 days
        LoanAccount account = LoanAccount.builder()
                .id(1L)
                .accountHolderName("Test User")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("10.00"))
                .interestAmount(new BigDecimal("821.917800")) // 30 days of interest
                .dateOfDisbursal(LocalDate.of(2024, 1, 1))
                .build();

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(account);

        // When
        BigDecimal appliedInterest = interestService.applyAccruedInterestToPrincipal(account);

        // Then
        assertEquals(new BigDecimal("821.917800"), appliedInterest);

        ArgumentCaptor<LoanAccount> accountCaptor = ArgumentCaptor.forClass(LoanAccount.class);
        verify(loanAccountRepository).save(accountCaptor.capture());

        LoanAccount savedAccount = accountCaptor.getValue();
        assertEquals(new BigDecimal("100821.917800"), savedAccount.getPrincipalAmount()); // Interest added to principal
        assertEquals(BigDecimal.ZERO, savedAccount.getInterestAmount()); // Interest amount reset
    }

    @Test
    void testApplyAccruedInterestToPrincipal_WithZeroInterest_DoesNotSave() {
        // Given: Account with zero accrued interest
        LoanAccount account = LoanAccount.builder()
                .id(1L)
                .accountHolderName("Test User")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("10.00"))
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2024, 1, 1))
                .build();

        // When
        BigDecimal appliedInterest = interestService.applyAccruedInterestToPrincipal(account);

        // Then
        assertEquals(BigDecimal.ZERO, appliedInterest);
        verify(loanAccountRepository, never()).save(any(LoanAccount.class));
    }

    @Test
    void testApplyMonthEndInterest_ProcessesAllAccounts() {
        // Given
        LocalDate monthEnd = LocalDate.of(2024, 1, 31);

        LoanAccount account1 = LoanAccount.builder()
                .id(1L)
                .principalAmount(new BigDecimal("100000.00"))
                .interestAmount(new BigDecimal("821.917800"))
                .build();

        LoanAccount account2 = LoanAccount.builder()
                .id(2L)
                .principalAmount(new BigDecimal("50000.00"))
                .interestAmount(new BigDecimal("410.958900"))
                .build();

        List<LoanAccount> accounts = Arrays.asList(account1, account2);
        Page<LoanAccount> page = new PageImpl<>(accounts, PageRequest.of(0, 100), 2);

        when(loanAccountRepository.count()).thenReturn(2L);
        when(loanAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(loanAccountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InterestService.InterestApplicationResult result = interestService.applyMonthEndInterest(monthEnd);

        // Then
        assertEquals(monthEnd, result.getDate());
        assertEquals(2, result.getTotalAccountsProcessed());
        assertEquals(0, result.getFailedAccounts());
        assertEquals(new BigDecimal("1232.876700"), result.getTotalInterestApplied());

        verify(loanAccountRepository, times(2)).save(any(LoanAccount.class));
    }

    @Test
    void testFullMonthCycle_DailyAccumulationAndMonthEndApplication() {
        // Given: Simulate a full month cycle
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        LoanAccount account = LoanAccount.builder()
                .id(1L)
                .accountHolderName("Test User")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("12.00")) // 12% annual
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(startDate)
                .build();

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(account);

        // When: Apply daily interest for 31 days
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            interestService.applyInterestToAccount(account, date);
        }

        // Then: After 31 days
        // Daily interest = (100000 * 0.12) / 365 = 32.876712
        // 31 days = 32.876712 * 31 = 1019.178072
        BigDecimal expectedMonthInterest = new BigDecimal("1019.178072");
        assertEquals(expectedMonthInterest, account.getInterestAmount());
        assertEquals(new BigDecimal("100000.00"), account.getPrincipalAmount()); // Principal unchanged during month

        // When: Apply month-end interest
        interestService.applyAccruedInterestToPrincipal(account);

        // Then: Interest should be added to principal and reset
        assertEquals(new BigDecimal("101019.178072"), account.getPrincipalAmount());
        assertEquals(BigDecimal.ZERO, account.getInterestAmount());
    }

    @Test
    void testCompoundingEffect_SecondMonth() {
        // Given: Account after first month with compounded principal
        LocalDate secondMonthStart = LocalDate.of(2024, 2, 1);

        LoanAccount account = LoanAccount.builder()
                .id(1L)
                .accountHolderName("Test User")
                .principalAmount(new BigDecimal("101019.178072")) // After first month compounding
                .interestRate(new BigDecimal("12.00"))
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2024, 1, 1))
                .build();

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(account);

        // When: Apply one day of interest on new principal
        BigDecimal dailyInterest = interestService.applyInterestToAccount(account, secondMonthStart);

        // Then: Daily interest should be calculated on the new higher principal
        // (101019.178072 * 0.12) / 365 = 33.211785
        BigDecimal expected = new BigDecimal("33.211785");
        assertEquals(expected, dailyInterest);
        assertTrue(dailyInterest.compareTo(new BigDecimal("32.876712")) > 0); // Higher than first month's daily interest
    }

    @Test
    void testDifferentInterestRates() {
        // Given: Different interest rates
        BigDecimal principal = new BigDecimal("100000.00");

        // When/Then: 5% rate
        BigDecimal daily5Percent = interestService.calculateDailyInterest(principal, new BigDecimal("5.00"));
        assertEquals(new BigDecimal("13.698630"), daily5Percent);

        // When/Then: 10% rate
        BigDecimal daily10Percent = interestService.calculateDailyInterest(principal, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("27.397260"), daily10Percent);

        // When/Then: 15% rate
        BigDecimal daily15Percent = interestService.calculateDailyInterest(principal, new BigDecimal("15.00"));
        assertEquals(new BigDecimal("41.095890"), daily15Percent);
    }

    @Test
    void testApplyMonthEndInterest_HandlesExceptions() {
        // Given
        LocalDate monthEnd = LocalDate.of(2024, 1, 31);

        LoanAccount failingAccount = LoanAccount.builder()
                .id(1L)
                .principalAmount(new BigDecimal("100000.00"))
                .interestAmount(new BigDecimal("821.917800"))
                .build();

        Page<LoanAccount> page = new PageImpl<>(Arrays.asList(failingAccount), PageRequest.of(0, 100), 1);

        when(loanAccountRepository.count()).thenReturn(1L);
        when(loanAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(loanAccountRepository.save(any(LoanAccount.class))).thenThrow(new RuntimeException("Database error"));

        // When
        InterestService.InterestApplicationResult result = interestService.applyMonthEndInterest(monthEnd);

        // Then
        assertEquals(1, result.getTotalAccountsProcessed());
        assertEquals(1, result.getFailedAccounts());
        assertEquals(BigDecimal.ZERO, result.getTotalInterestApplied());
    }
}
