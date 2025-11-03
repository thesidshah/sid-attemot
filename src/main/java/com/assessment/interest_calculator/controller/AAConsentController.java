package com.assessment.interest_calculator.controller;

import com.assessment.interest_calculator.dto.ConsentStatusResponse;
import com.assessment.interest_calculator.dto.InitiateConsentRequest;
import com.assessment.interest_calculator.service.AAConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/aa/consent")
@Slf4j
@RequiredArgsConstructor
public class AAConsentController {

    private final AAConsentService consentService;

    /**
     * Initiates a consent request for a loan account
     * POST /api/aa/consent/initiate
     */
    @PostMapping("/initiate")
    public Mono<ResponseEntity<ConsentStatusResponse>> initiateConsent(
            @Valid @RequestBody InitiateConsentRequest request) {
        log.info("API: Initiating consent for loan account: {}", request.getLoanAccountId());

        return consentService.initiateConsent(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.error("Invalid request: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(error -> {
                    log.error("Error initiating consent: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Gets the current status of a consent request
     * GET /api/aa/consent/{consentRequestId}/status
     */
    @GetMapping("/{consentRequestId}/status")
    public Mono<ResponseEntity<ConsentStatusResponse>> getConsentStatus(
            @PathVariable Long consentRequestId) {
        log.info("API: Fetching consent status for ID: {}", consentRequestId);

        return consentService.getConsentStatus(consentRequestId)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.error("Consent request not found: {}", error.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(error -> {
                    log.error("Error fetching consent status: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
