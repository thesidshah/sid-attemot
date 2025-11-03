package com.assessment.interest_calculator.controller;

import com.assessment.interest_calculator.dto.FIRequestDetailsResponseDTO;
import com.assessment.interest_calculator.dto.FIRequestResponseDTO;
import com.assessment.interest_calculator.entity.FIPAccount;
import com.assessment.interest_calculator.entity.FinancialData;
import com.assessment.interest_calculator.service.AADataFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/aa/fi")
@Slf4j
@RequiredArgsConstructor
public class AADataController {

    private final AADataFetchService dataFetchService;

    /**
     * Requests financial data for an approved consent
     * POST /api/aa/fi/request/{consentRequestId}
     */
    @PostMapping("/request/{consentRequestId}")
    public Mono<ResponseEntity<FIRequestResponseDTO>> requestFinancialData(
            @PathVariable Long consentRequestId) {
        log.info("API: Requesting FI data for consent: {}", consentRequestId);

        return dataFetchService.requestFinancialData(consentRequestId)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.error("Invalid request: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(IllegalStateException.class, error -> {
                    log.error("Invalid state: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
                })
                .onErrorResume(error -> {
                    log.error("Error requesting FI data: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Gets the status of an FI request
     * GET /api/aa/fi/request/{fiRequestId}/status
     */
    @GetMapping("/request/{fiRequestId}/status")
    public Mono<ResponseEntity<FIRequestDetailsResponseDTO>> getFIRequestStatus(
            @PathVariable String fiRequestId) {
        log.info("API: Fetching FI request status for: {}", fiRequestId);

        return dataFetchService.getFIRequestStatus(fiRequestId)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.error("FI request not found: {}", error.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(error -> {
                    log.error("Error fetching FI request status: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Fetches and stores financial data for an FI request
     * POST /api/aa/fi/fetch/{fiRequestId}
     */
    @PostMapping("/fetch/{fiRequestId}")
    public Mono<ResponseEntity<FinancialData>> fetchFinancialData(
            @PathVariable String fiRequestId) {
        log.info("API: Fetching financial data for FI request: {}", fiRequestId);

        return dataFetchService.fetchAndStoreFinancialData(fiRequestId)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.error("FI request not found: {}", error.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(error -> {
                    log.error("Error fetching financial data: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Gets financial data for a consent request
     * GET /api/aa/consent/{consentRequestId}/data
     */
    @GetMapping("/consent/{consentRequestId}/data")
    public Mono<ResponseEntity<FinancialData>> getFinancialData(
            @PathVariable Long consentRequestId) {
        log.info("API: Retrieving financial data for consent: {}", consentRequestId);

        return dataFetchService.getFinancialData(consentRequestId)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.error("Data not found: {}", error.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(error -> {
                    log.error("Error retrieving financial data: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Gets FIP accounts for financial data
     * GET /api/aa/data/{financialDataId}/accounts
     */
    @GetMapping("/data/{financialDataId}/accounts")
    public Mono<ResponseEntity<List<FIPAccount>>> getFIPAccounts(
            @PathVariable Long financialDataId) {
        log.info("API: Retrieving FIP accounts for financial data: {}", financialDataId);

        return dataFetchService.getFIPAccounts(financialDataId)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error retrieving FIP accounts: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
