package com.assessment.interest_calculator.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.LoanAccountRepository;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanAccountRepository loanAccountRepository;

    private LoanAccount testAccount;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now();
        testAccount = LoanAccount.builder()
                .id(1L)
                .accountHolderName("John Doe")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("5.5"))
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2025, 1, 1))
                .version(0L)
                .build();
        testAccount.setCreatedAt(now);
        testAccount.setUpdatedAt(now);
    }

    @Test
    void testCreateAccount_WithValidData_ShouldReturnCreated() throws Exception {
        // Arrange
        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(testAccount);

        String requestBody = """
                {
                    "accountHolderName": "John Doe",
                    "principalAmount": 100000.00,
                    "interestRate": 5.5,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.principalAmount").value(100000.00))
                .andExpect(jsonPath("$.interestRate").value(5.5))
                .andExpect(jsonPath("$.interestAmount").value(0))
                .andExpect(jsonPath("$.dateOfDisbursal").value("2025-01-01"));

        verify(loanAccountRepository, times(1)).save(any(LoanAccount.class));
    }

    @Test
    void testCreateAccount_WithMissingName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "principalAmount": 100000.00,
                    "interestRate": 5.5,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(loanAccountRepository, times(0)).save(any(LoanAccount.class));
    }

    @Test
    void testCreateAccount_WithBlankName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "accountHolderName": "   ",
                    "principalAmount": 100000.00,
                    "interestRate": 5.5,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAccount_WithNegativePrincipal_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "accountHolderName": "John Doe",
                    "principalAmount": -1000.00,
                    "interestRate": 5.5,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAccount_WithInterestRateAbove100_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "accountHolderName": "John Doe",
                    "principalAmount": 100000.00,
                    "interestRate": 150.0,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAccount_WithNegativeInterestRate_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "accountHolderName": "John Doe",
                    "principalAmount": 100000.00,
                    "interestRate": -5.0,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAccount_WithMissingDateOfDisbursal_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "accountHolderName": "John Doe",
                    "principalAmount": 100000.00,
                    "interestRate": 5.5
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllAccounts_ShouldReturnPagedAccounts() throws Exception {
        // Arrange
        LoanAccount account2 = LoanAccount.builder()
                .id(2L)
                .accountHolderName("Jane Smith")
                .principalAmount(new BigDecimal("50000.00"))
                .interestRate(new BigDecimal("4.5"))
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2025, 1, 15))
                .version(0L)
                .build();
        account2.setCreatedAt(now);
        account2.setUpdatedAt(now);

        List<LoanAccount> accounts = Arrays.asList(testAccount, account2);
        Page<LoanAccount> page = new PageImpl<>(accounts);

        when(loanAccountRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].accountHolderName").value("Jane Smith"));

        verify(loanAccountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllAccounts_WithPagination_ShouldUseProvidedPageAndSize() throws Exception {
        // Arrange
        List<LoanAccount> accounts = Arrays.asList(testAccount);
        Page<LoanAccount> page = new PageImpl<>(accounts, PageRequest.of(1, 10), 50);

        when(loanAccountRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/accounts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(loanAccountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllAccounts_WhenEmpty_ShouldReturnEmptyList() throws Exception {
        // Arrange
        Page<LoanAccount> emptyPage = new PageImpl<>(Arrays.asList());
        when(loanAccountRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetAccountById_WhenExists_ShouldReturnAccount() throws Exception {
        // Arrange
        when(loanAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.principalAmount").value(100000.00))
                .andExpect(jsonPath("$.interestRate").value(5.5));

        verify(loanAccountRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAccountById_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(loanAccountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/accounts/999"))
                .andExpect(status().isNotFound());

        verify(loanAccountRepository, times(1)).findById(999L);
    }

    @Test
    void testCreateAccount_WithZeroInterestRate_ShouldSucceed() throws Exception {
        // Arrange
        LoanAccount zeroInterestAccount = LoanAccount.builder()
                .id(3L)
                .accountHolderName("Zero Interest")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(BigDecimal.ZERO)
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2025, 1, 1))
                .version(0L)
                .build();
        zeroInterestAccount.setCreatedAt(now);
        zeroInterestAccount.setUpdatedAt(now);

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(zeroInterestAccount);

        String requestBody = """
                {
                    "accountHolderName": "Zero Interest",
                    "principalAmount": 100000.00,
                    "interestRate": 0.0,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interestRate").value(0.0));
    }

    @Test
    void testCreateAccount_WithMaxInterestRate_ShouldSucceed() throws Exception {
        // Arrange
        LoanAccount maxInterestAccount = LoanAccount.builder()
                .id(4L)
                .accountHolderName("Max Interest")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("100.0"))
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2025, 1, 1))
                .version(0L)
                .build();
        maxInterestAccount.setCreatedAt(now);
        maxInterestAccount.setUpdatedAt(now);

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(maxInterestAccount);

        String requestBody = """
                {
                    "accountHolderName": "Max Interest",
                    "principalAmount": 100000.00,
                    "interestRate": 100.0,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interestRate").value(100.0));
    }

    @Test
    void testCreateAccount_WithHighPrecisionValues_ShouldMaintainPrecision() throws Exception {
        // Arrange
        LoanAccount precisionAccount = LoanAccount.builder()
                .id(5L)
                .accountHolderName("Precision Test")
                .principalAmount(new BigDecimal("123456.789012"))
                .interestRate(new BigDecimal("7.654321"))
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(LocalDate.of(2025, 1, 1))
                .version(0L)
                .build();
        precisionAccount.setCreatedAt(now);
        precisionAccount.setUpdatedAt(now);

        when(loanAccountRepository.save(any(LoanAccount.class))).thenReturn(precisionAccount);

        String requestBody = """
                {
                    "accountHolderName": "Precision Test",
                    "principalAmount": 123456.789012,
                    "interestRate": 7.654321,
                    "dateOfDisbursal": "2025-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principalAmount").value(123456.789012))
                .andExpect(jsonPath("$.interestRate").value(7.654321));
    }
}
