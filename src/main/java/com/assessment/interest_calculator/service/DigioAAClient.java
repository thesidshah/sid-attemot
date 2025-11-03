package com.assessment.interest_calculator.service;

import com.assessment.interest_calculator.config.DigioAAProperties;
import com.assessment.interest_calculator.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigioAAClient {
    
    private final WebClient digioWebClient;
    private final DigioAAProperties properties;
    
    public Mono<ConsentResponseDTO> createConsentRequest(ConsentRequestDTO request) {
        log.info("Creating consent request with template: {}", request.getTemplateId());
        
        return digioWebClient
            .post()
            .uri("/client/consent/request")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ConsentResponseDTO.class)
            .doOnSuccess(response -> log.info("Consent created: {}", 
                response != null ? response.getConsentRequestId() : "null"))
            .doOnError(error -> log.error("Error creating consent: {}", error.getMessage()));
    }
    
    public Mono<ConsentDetailsResponseDTO> getConsentDetails(String consentRequestId) {
        log.info("Fetching consent details for: {}", consentRequestId);
        
        return digioWebClient
            .get()
            .uri("/client/consent/request/{consent_request_id}/details", consentRequestId)
            .retrieve()
            .bodyToMono(ConsentDetailsResponseDTO.class)
            .doOnSuccess(response -> log.info("Consent status: {}", 
                response != null ? response.getStatus() : "null"))
            .doOnError(error -> log.error("Error fetching consent details: {}", error.getMessage()));
    }
    
    public Mono<FIRequestResponseDTO> requestFinancialInfo(
            String consentRequestId, 
            String fromDate, 
            String toDate) {
        
        log.info("Requesting FI for consent: {} from {} to {}", 
                 consentRequestId, fromDate, toDate);
        
        FIRequestDTO requestBody = FIRequestDTO.builder()
            .from(fromDate)
            .to(toDate)
            .build();
        
        return digioWebClient
            .post()
            .uri("/client/fi/request/{consent_request_id}", consentRequestId)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(FIRequestResponseDTO.class)
            .doOnSuccess(response -> {
                if (response != null) {
                    log.info("FI request created - ID: {}, Status: {}", 
                            response.getFiRequestId(), response.getStatus());
                }
            })
            .doOnError(error -> log.error("Error requesting FI: {}", error.getMessage()));
    }
    
    public Mono<FIRequestDetailsResponseDTO> getFIRequestDetails(String fiRequestId) {
        log.info("Fetching FI request details for: {}", fiRequestId);
        
        return digioWebClient
            .get()
            .uri("/client/fi/request/{fi_request_id}/details", fiRequestId)
            .retrieve()
            .bodyToMono(FIRequestDetailsResponseDTO.class)
            .doOnSuccess(response -> log.info("FI request status: {}", 
                response != null ? response.getStatus() : "null"))
            .doOnError(error -> log.error("Error fetching FI request details: {}", error.getMessage()));
    }
    
    public Mono<FIFetchResponseDTO> fetchFinancialDataByFiRequestId(String fiRequestId) {
        log.info("Fetching financial data for FI request: {}", fiRequestId);
        
        return digioWebClient
            .get()
            .uri("/client/fi/fetch/{fi_request_id}", fiRequestId)
            .retrieve()
            .bodyToMono(FIFetchResponseDTO.class)
            .doOnSuccess(response -> {
                if (response != null) {
                    log.info("FI data fetched - Status: {}, Accounts: {}", 
                        response.getStatus(), 
                        response.getFi() != null ? response.getFi().size() : 0);
                }
            })
            .doOnError(error -> log.error("Error fetching FI data: {}", error.getMessage()));
    }
    
    public Mono<FIFetchResponseDTO> fetchFinancialDataByConsentId(String consentRequestId) {
        log.info("Fetching financial data for consent: {}", consentRequestId);
        
        return digioWebClient
            .get()
            .uri("/client/consent/{consent_request_id}/data", consentRequestId)
            .retrieve()
            .bodyToMono(FIFetchResponseDTO.class)
            .doOnSuccess(response -> {
                if (response != null) {
                    log.info("FI data fetched - Status: {}, Accounts: {}", 
                        response.getStatus(), 
                        response.getFi() != null ? response.getFi().size() : 0);
                }
            })
            .doOnError(error -> log.error("Error fetching FI data: {}", error.getMessage()));
    }
}
