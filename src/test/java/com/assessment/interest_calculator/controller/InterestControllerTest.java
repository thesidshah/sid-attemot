package com.assessment.interest_calculator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.assessment.interest_calculator.service.InterestService;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterestService interestService;

    private InterestService.InterestApplicationResult mockResult;

    @BeforeEach
    void setUp() {
        mockResult = InterestService.InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(10)
                .failedAccounts(0)
                .totalInterestApplied(new BigDecimal("150.50"))
                .durationMs(1000L)
                .build();
    }

    @Test
    void testApplyDailyInterest_WithoutDate_ShouldUseCurrentDate() throws Exception {
        // Arrange
        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/api/interest/apply-daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccountsProcessed").value(10))
                .andExpect(jsonPath("$.failedAccounts").value(0))
                .andExpect(jsonPath("$.totalInterestApplied").value(150.50))
                .andExpect(jsonPath("$.durationMs").value(1000));

        verify(interestService, times(1)).applyDailyInterest(any(LocalDate.class));
    }

    @Test
    void testApplyDailyInterest_WithSpecificDate_ShouldUseProvidedDate() throws Exception {
        // Arrange
        LocalDate specificDate = LocalDate.of(2025, 1, 15);
        InterestService.InterestApplicationResult specificResult = InterestService.InterestApplicationResult.builder()
                .date(specificDate)
                .totalAccountsProcessed(5)
                .failedAccounts(1)
                .totalInterestApplied(new BigDecimal("75.25"))
                .durationMs(500L)
                .build();

        when(interestService.applyDailyInterest(specificDate)).thenReturn(specificResult);

        // Act & Assert
        mockMvc.perform(post("/api/interest/apply-daily")
                        .param("date", "2025-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2025-01-15"))
                .andExpect(jsonPath("$.totalAccountsProcessed").value(5))
                .andExpect(jsonPath("$.failedAccounts").value(1))
                .andExpect(jsonPath("$.totalInterestApplied").value(75.25));

        verify(interestService, times(1)).applyDailyInterest(specificDate);
    }

    @Test
    void testApplyDailyInterest_WhenNoAccountsProcessed_ShouldReturnZeros() throws Exception {
        // Arrange
        InterestService.InterestApplicationResult emptyResult = InterestService.InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(0)
                .failedAccounts(0)
                .totalInterestApplied(BigDecimal.ZERO)
                .durationMs(100L)
                .build();

        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(emptyResult);

        // Act & Assert
        mockMvc.perform(post("/api/interest/apply-daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccountsProcessed").value(0))
                .andExpect(jsonPath("$.failedAccounts").value(0))
                .andExpect(jsonPath("$.totalInterestApplied").value(0));
    }

    @Test
    void testApplyMonthEndInterest_WithoutDate_ShouldUseCurrentDate() throws Exception {
        // Arrange
        when(interestService.applyMonthEndInterest(any(LocalDate.class))).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/api/interest/apply-month-end"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccountsProcessed").value(10))
                .andExpect(jsonPath("$.failedAccounts").value(0))
                .andExpect(jsonPath("$.totalInterestApplied").value(150.50));

        verify(interestService, times(1)).applyMonthEndInterest(any(LocalDate.class));
    }

    @Test
    void testApplyMonthEndInterest_WithSpecificDate_ShouldUseProvidedDate() throws Exception {
        // Arrange
        LocalDate monthEndDate = LocalDate.of(2025, 1, 31);
        InterestService.InterestApplicationResult monthEndResult = InterestService.InterestApplicationResult.builder()
                .date(monthEndDate)
                .totalAccountsProcessed(20)
                .failedAccounts(2)
                .totalInterestApplied(new BigDecimal("1500.75"))
                .durationMs(2000L)
                .build();

        when(interestService.applyMonthEndInterest(monthEndDate)).thenReturn(monthEndResult);

        // Act & Assert
        mockMvc.perform(post("/api/interest/apply-month-end")
                        .param("date", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2025-01-31"))
                .andExpect(jsonPath("$.totalAccountsProcessed").value(20))
                .andExpect(jsonPath("$.failedAccounts").value(2))
                .andExpect(jsonPath("$.totalInterestApplied").value(1500.75));

        verify(interestService, times(1)).applyMonthEndInterest(monthEndDate);
    }

    @Test
    void testApplyMonthEndInterest_WithPartialFailures_ShouldReturnFailureCount() throws Exception {
        // Arrange
        InterestService.InterestApplicationResult partialFailureResult = InterestService.InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(100)
                .failedAccounts(5)
                .totalInterestApplied(new BigDecimal("5000.00"))
                .durationMs(3000L)
                .build();

        when(interestService.applyMonthEndInterest(any(LocalDate.class))).thenReturn(partialFailureResult);

        // Act & Assert
        mockMvc.perform(post("/api/interest/apply-month-end"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccountsProcessed").value(100))
                .andExpect(jsonPath("$.failedAccounts").value(5))
                .andExpect(jsonPath("$.totalInterestApplied").value(5000.00));
    }

    @Test
    void testApplyDailyInterest_WithLargeInterestAmount_ShouldHandlePrecision() throws Exception {
        // Arrange
        InterestService.InterestApplicationResult largeResult = InterestService.InterestApplicationResult.builder()
                .date(LocalDate.now())
                .totalAccountsProcessed(1000)
                .failedAccounts(0)
                .totalInterestApplied(new BigDecimal("123456.789012"))
                .durationMs(5000L)
                .build();

        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(largeResult);

        // Act & Assert
        mockMvc.perform(post("/api/interest/apply-daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInterestApplied").value(123456.789012));
    }

    @Test
    void testApplyDailyInterest_ServiceMethodCalledOnce() throws Exception {
        // Arrange
        when(interestService.applyDailyInterest(any(LocalDate.class))).thenReturn(mockResult);

        // Act
        mockMvc.perform(post("/api/interest/apply-daily"));

        // Assert
        verify(interestService, times(1)).applyDailyInterest(any(LocalDate.class));
    }

    @Test
    void testApplyMonthEndInterest_ServiceMethodCalledOnce() throws Exception {
        // Arrange
        when(interestService.applyMonthEndInterest(any(LocalDate.class))).thenReturn(mockResult);

        // Act
        mockMvc.perform(post("/api/interest/apply-month-end"));

        // Assert
        verify(interestService, times(1)).applyMonthEndInterest(any(LocalDate.class));
    }
}
