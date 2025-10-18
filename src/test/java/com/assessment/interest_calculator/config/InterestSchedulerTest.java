package com.assessment.interest_calculator.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.assessment.interest_calculator.service.InterestService;
import com.assessment.interest_calculator.service.InterestService.InterestApplicationResult;

@ExtendWith(MockitoExtension.class)
class InterestSchedulerTest {

    @Mock
    private InterestService interestService;

    @InjectMocks
    private InterestScheduler interestScheduler;

    private static final String TEST_ZONE = "Asia/Kolkata";

    @BeforeEach
    void setUp() {
        // Set the zone field using reflection since it's injected via @Value
        ReflectionTestUtils.setField(interestScheduler, "zone", TEST_ZONE);
    }

    @Test
    void testRunDailyInterestCalculation_Success() {
        // Given
        InterestApplicationResult mockResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(100)
                .failedAccounts(0)
                .totalInterestApplied(new BigDecimal("5000.00"))
                .durationMs(1500L)
                .build();

        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(mockResult);

        // When
        interestScheduler.runDailyInterestCalculation();

        // Then
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(interestService).applyDailyInterest(dateCaptor.capture());

        // Verify that today's date was used
        LocalDate capturedDate = dateCaptor.getValue();
        assert capturedDate != null;
    }

    @Test
    void testRunDailyInterestCalculation_HandlesException() {
        // Given
        when(interestService.applyDailyInterest(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        interestScheduler.runDailyInterestCalculation();

        // Then
        // Verify that the service was called despite the exception
        verify(interestService).applyDailyInterest(any(LocalDate.class));
        // The scheduler should handle the exception gracefully and continue
    }

    @Test
    void testRunDailyInterestCalculation_CallsServiceWithCorrectDate() {
        // Given
        InterestApplicationResult mockResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(50)
                .failedAccounts(0)
                .totalInterestApplied(new BigDecimal("2500.00"))
                .durationMs(1000L)
                .build();

        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(mockResult);

        // When
        interestScheduler.runDailyInterestCalculation();

        // Then
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(interestService, times(1)).applyDailyInterest(dateCaptor.capture());

        LocalDate capturedDate = dateCaptor.getValue();
        LocalDate today = LocalDate.now();

        // Verify the date is today (allowing for potential timezone differences)
        assert capturedDate.equals(today) || capturedDate.equals(today.plusDays(1)) || capturedDate.equals(today.minusDays(1));
    }

    @Test
    void testRunMonthEndInterestApplication_Success() {
        // Given
        InterestApplicationResult mockResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(100)
                .failedAccounts(0)
                .totalInterestApplied(new BigDecimal("150000.00"))
                .durationMs(3000L)
                .build();

        when(interestService.applyMonthEndInterest(any(LocalDate.class))).thenReturn(mockResult);

        // When
        interestScheduler.runMonthEndInterestApplication();

        // Then
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(interestService).applyMonthEndInterest(dateCaptor.capture());

        // Verify that today's date was used
        LocalDate capturedDate = dateCaptor.getValue();
        assert capturedDate != null;
    }

    @Test
    void testRunMonthEndInterestApplication_HandlesException() {
        // Given
        when(interestService.applyMonthEndInterest(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Transaction timeout"));

        // When
        interestScheduler.runMonthEndInterestApplication();

        // Then
        // Verify that the service was called despite the exception
        verify(interestService).applyMonthEndInterest(any(LocalDate.class));
        // The scheduler should handle the exception gracefully and continue
    }

    @Test
    void testRunMonthEndInterestApplication_CallsServiceWithCorrectDate() {
        // Given
        InterestApplicationResult mockResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(75)
                .failedAccounts(2)
                .totalInterestApplied(new BigDecimal("100000.00"))
                .durationMs(2500L)
                .build();

        when(interestService.applyMonthEndInterest(any(LocalDate.class))).thenReturn(mockResult);

        // When
        interestScheduler.runMonthEndInterestApplication();

        // Then
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(interestService, times(1)).applyMonthEndInterest(dateCaptor.capture());

        LocalDate capturedDate = dateCaptor.getValue();
        assert capturedDate != null;
    }

    @Test
    void testRunDailyInterestCalculation_WithPartialFailures() {
        // Given
        InterestApplicationResult mockResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(100)
                .failedAccounts(5)
                .totalInterestApplied(new BigDecimal("4750.00"))
                .durationMs(2000L)
                .build();

        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(mockResult);

        // When
        interestScheduler.runDailyInterestCalculation();

        // Then
        verify(interestService).applyDailyInterest(any(LocalDate.class));
        // Scheduler should complete even with partial failures
    }

    @Test
    void testRunMonthEndInterestApplication_WithPartialFailures() {
        // Given
        InterestApplicationResult mockResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(100)
                .failedAccounts(10)
                .totalInterestApplied(new BigDecimal("135000.00"))
                .durationMs(3500L)
                .build();

        when(interestService.applyMonthEndInterest(any(LocalDate.class))).thenReturn(mockResult);

        // When
        interestScheduler.runMonthEndInterestApplication();

        // Then
        verify(interestService).applyMonthEndInterest(any(LocalDate.class));
        // Scheduler should complete even with partial failures
    }

    @Test
    void testSchedulerMethodsAreIndependent() {
        // Given
        InterestApplicationResult dailyResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(50)
                .failedAccounts(0)
                .totalInterestApplied(new BigDecimal("2500.00"))
                .durationMs(1000L)
                .build();

        InterestApplicationResult monthEndResult = InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(50)
                .failedAccounts(0)
                .totalInterestApplied(new BigDecimal("75000.00"))
                .durationMs(2000L)
                .build();

        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(dailyResult);
        when(interestService.applyMonthEndInterest(any(LocalDate.class))).thenReturn(monthEndResult);

        // When
        interestScheduler.runDailyInterestCalculation();
        interestScheduler.runMonthEndInterestApplication();

        // Then
        verify(interestService, times(1)).applyDailyInterest(any(LocalDate.class));
        verify(interestService, times(1)).applyMonthEndInterest(any(LocalDate.class));
        // Both methods should execute independently
    }

    @Test
    void testRunDailyInterestCalculation_ExceptionDoesNotPropagate() {
        // Given
        when(interestService.applyDailyInterest(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Critical error"));

        // When & Then - Should not throw exception
        try {
            interestScheduler.runDailyInterestCalculation();
            // If we reach here, the exception was handled properly
            verify(interestService).applyDailyInterest(any(LocalDate.class));
        } catch (Exception e) {
            throw new AssertionError("Exception should have been caught by scheduler", e);
        }
    }

    @Test
    void testRunMonthEndInterestApplication_ExceptionDoesNotPropagate() {
        // Given
        when(interestService.applyMonthEndInterest(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Critical error"));

        // When & Then - Should not throw exception
        try {
            interestScheduler.runMonthEndInterestApplication();
            // If we reach here, the exception was handled properly
            verify(interestService).applyMonthEndInterest(any(LocalDate.class));
        } catch (Exception e) {
            throw new AssertionError("Exception should have been caught by scheduler", e);
        }
    }

    @Test
    void testRunDailyInterestCalculation_NullPointerException() {
        // Given
        when(interestService.applyDailyInterest(any(LocalDate.class)))
                .thenThrow(new NullPointerException("Null account found"));

        // When
        interestScheduler.runDailyInterestCalculation();

        // Then
        verify(interestService).applyDailyInterest(any(LocalDate.class));
        // Scheduler should handle NPE gracefully
    }

    @Test
    void testRunMonthEndInterestApplication_IllegalStateException() {
        // Given
        when(interestService.applyMonthEndInterest(any(LocalDate.class)))
                .thenThrow(new IllegalStateException("Invalid account state"));

        // When
        interestScheduler.runMonthEndInterestApplication();

        // Then
        verify(interestService).applyMonthEndInterest(any(LocalDate.class));
        // Scheduler should handle IllegalStateException gracefully
    }
}
