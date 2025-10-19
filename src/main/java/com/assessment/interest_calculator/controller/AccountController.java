package com.assessment.interest_calculator.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.assessment.interest_calculator.dto.AccountResponse;
import com.assessment.interest_calculator.dto.CreateAccountRequest;
import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.LoanAccountRepository;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class AccountController {
    private final LoanAccountRepository loanAccountRepository;

    public AccountController(LoanAccountRepository loanAccountRepository) {
        this.loanAccountRepository = loanAccountRepository;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("Creating new loan account for: {}", request.getAccountHolderName());

        LoanAccount account = LoanAccount.builder()
                .accountHolderName(request.getAccountHolderName())
                .principalAmount(request.getPrincipalAmount())
                .interestRate(request.getInterestRate())
                .interestAmount(BigDecimal.ZERO)
                .dateOfDisbursal(request.getDateOfDisbursal())
                .build();

        LoanAccount savedAccount = loanAccountRepository.save(account);
        log.info("Created loan account with ID: {}", savedAccount.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toAccountResponse(savedAccount));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching all accounts - page: {}, size: {}", page, size);

        Page<LoanAccount> accountsPage = loanAccountRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<AccountResponse> accounts = accountsPage.getContent().stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        log.info("Fetching account with ID: {}", id);

        return loanAccountRepository.findById(id)
                .map(account -> ResponseEntity.ok(toAccountResponse(account)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AccountResponse toAccountResponse(LoanAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountHolderName(account.getAccountHolderName())
                .principalAmount(account.getPrincipalAmount())
                .interestRate(account.getInterestRate())
                .interestAmount(account.getInterestAmount())
                .dateOfDisbursal(account.getDateOfDisbursal())
                .lastInterestAppliedAt(account.getLastInterestAppliedAt())
                .version(account.getVersion())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
