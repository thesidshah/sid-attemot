package com.assessment.interest_calculator.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.assessment.interest_calculator.service.InterestService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/interest")
@Slf4j
public class InterestController {
    private final InterestService interestService;

    public InterestController(InterestService interestService) {
        this.interestService = interestService;
    }

    @PostMapping("/apply-daily")
    public ResponseEntity<InterestService.InterestApplicationResult> applyDailyInterest(
            @RequestParam(required = false) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("Manually triggering daily interest application for date: {}", targetDate);

        InterestService.InterestApplicationResult result = interestService.applyDailyInterest(targetDate);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/apply-month-end")
    public ResponseEntity<InterestService.InterestApplicationResult> applyMonthEndInterest(
            @RequestParam(required = false) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("Manually triggering month-end interest application for date: {}", targetDate);

        InterestService.InterestApplicationResult result = interestService.applyMonthEndInterest(targetDate);

        return ResponseEntity.ok(result);
    }
}
