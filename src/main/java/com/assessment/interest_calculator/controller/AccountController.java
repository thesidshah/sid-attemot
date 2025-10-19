package com.assessment.interest_calculator.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assessment.interest_calculator.repository.LoanAccountRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class AccountController {
      private final LoanAccountRepository loanAccountRepository;
      public AccountController(LoanAccountRepository loanAccountRepository) {
            this.loanAccountRepository = loanAccountRepository;
      }

}
