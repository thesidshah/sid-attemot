package com.assessment.interest_calculator.service;

import com.assessment.interest_calculator.dto.FIFetchResponseDTO;
import com.assessment.interest_calculator.dto.FIRequestDetailsResponseDTO;
import com.assessment.interest_calculator.dto.FIRequestResponseDTO;
import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.FIPAccount;
import com.assessment.interest_calculator.entity.FinancialData;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;
import com.assessment.interest_calculator.repository.FIPAccountRepository;
import com.assessment.interest_calculator.repository.FinancialDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AADataFetchService {

    private final DigioAAClient digioAAClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final FinancialDataRepository financialDataRepository;
    private final FIPAccountRepository fipAccountRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Requests financial data for an approved consent
     */
    @Transactional
    public Mono<FIRequestResponseDTO> requestFinancialData(Long consentRequestId) {
        log.info("Requesting FI data for consent: {}", consentRequestId);

        return Mono.fromCallable(() -> {
                    ConsentRequest consentRequest = consentRequestRepository.findById(consentRequestId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Consent request not found: " + consentRequestId));

                    // Validate consent is approved
                    if (consentRequest.getStatus() != ConsentRequest.ConsentStatus.APPROVED) {
                        throw new IllegalStateException(
                                "Consent must be APPROVED to request FI data. Current status: " +
                                        consentRequest.getStatus());
                    }

                    if (consentRequest.getConsentRequestId() == null) {
                        throw new IllegalStateException("Consent has no Digio consent request ID");
                    }

                    return consentRequest;
                })
                .flatMap(consentRequest -> {
                    String fromDate = consentRequest.getFiStartDate().format(DATE_FORMATTER);
                    String toDate = consentRequest.getFiEndDate().format(DATE_FORMATTER);

                    return digioAAClient.requestFinancialInfo(
                                    consentRequest.getConsentRequestId(),
                                    fromDate,
                                    toDate
                            )
                            .flatMap(response -> Mono.fromCallable(() -> {
                                // Update consent request with FI request details
                                consentRequest.setFiRequestId(response.getFiRequestId());
                                consentRequest.setFiRequestStatus(response.getStatus());
                                consentRequest.setFiRequestedAt(OffsetDateTime.now());
                                consentRequestRepository.save(consentRequest);

                                log.info("FI request created - ID: {}, Status: {}",
                                        response.getFiRequestId(), response.getStatus());

                                return response;
                            }));
                })
                .onErrorResume(error -> {
                    log.error("Error requesting FI data: {}", error.getMessage(), error);
                    return Mono.error(new RuntimeException("Failed to request FI data: " + error.getMessage()));
                });
    }

    /**
     * Gets the status of an FI request
     */
    @Transactional(readOnly = true)
    public Mono<FIRequestDetailsResponseDTO> getFIRequestStatus(String fiRequestId) {
        log.info("Fetching FI request status for: {}", fiRequestId);

        return Mono.fromCallable(() ->
                        consentRequestRepository.findByFiRequestId(fiRequestId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "FI request not found: " + fiRequestId))
                )
                .flatMap(consentRequest ->
                        digioAAClient.getFIRequestDetails(fiRequestId)
                                .flatMap(details -> Mono.fromCallable(() -> {
                                    // Update local FI request status
                                    consentRequest.setFiRequestStatus(details.getStatus());
                                    consentRequestRepository.save(consentRequest);

                                    return details;
                                }))
                                .onErrorResume(error -> {
                                    log.error("Error fetching FI request details: {}", error.getMessage());
                                    return Mono.error(new RuntimeException(
                                            "Failed to fetch FI request status: " + error.getMessage()));
                                })
                );
    }

    /**
     * Fetches and stores financial data for an FI request
     */
    @Transactional
    public Mono<FinancialData> fetchAndStoreFinancialData(String fiRequestId) {
        log.info("Fetching and storing financial data for FI request: {}", fiRequestId);

        return Mono.fromCallable(() ->
                        consentRequestRepository.findByFiRequestId(fiRequestId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "FI request not found: " + fiRequestId))
                )
                .flatMap(consentRequest ->
                        digioAAClient.fetchFinancialDataByFiRequestId(fiRequestId)
                                .flatMap(fiResponse -> Mono.fromCallable(() -> {
                                    try {
                                        return storeFinancialData(consentRequest, fiResponse);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException("Error processing financial data", e);
                                    }
                                }))
                )
                .onErrorResume(error -> {
                    log.error("Error fetching financial data: {}", error.getMessage(), error);
                    return Mono.error(new RuntimeException(
                            "Failed to fetch and store financial data: " + error.getMessage()));
                });
    }

    /**
     * Stores financial data and associated FIP accounts
     */
    private FinancialData storeFinancialData(
            ConsentRequest consentRequest,
            FIFetchResponseDTO fiResponse) throws JsonProcessingException {

        // Check if financial data already exists
        FinancialData financialData = financialDataRepository.findByConsentRequest(consentRequest)
                .orElse(null);

        if (financialData == null) {
            // Create new financial data record
            financialData = FinancialData.builder()
                    .consentRequest(consentRequest)
                    .fiRequestId(fiResponse.getFiRequestId())
                    .build();
        }

        // Update financial data
        financialData.setFiStatus(fiResponse.getStatus());
        financialData.setRawResponse(objectMapper.writeValueAsString(fiResponse));
        financialData.setDataFetchedAt(OffsetDateTime.now());
        financialData.setDataExpiresAt(OffsetDateTime.now().plusDays(30)); // Data valid for 30 days
        financialData.setIsPurged(false);

        // Count FIPs and accounts
        int accountCount = fiResponse.getFi() != null ? fiResponse.getFi().size() : 0;
        financialData.setAccountCount(accountCount);
        financialData.setFipCount(accountCount); // Each account is from a FIP

        financialData = financialDataRepository.save(financialData);

        // Store individual FIP accounts
        if (fiResponse.getFi() != null && !fiResponse.getFi().isEmpty()) {
            storeFIPAccounts(financialData, fiResponse.getFi());
        }

        // Update consent request status
        consentRequest.setStatus(ConsentRequest.ConsentStatus.DATA_FETCHED);
        consentRequestRepository.save(consentRequest);

        log.info("Stored financial data - ID: {}, Accounts: {}, FIPs: {}",
                financialData.getId(), financialData.getAccountCount(), financialData.getFipCount());

        return financialData;
    }

    /**
     * Stores individual FIP account records
     */
    private void storeFIPAccounts(
            FinancialData financialData,
            List<FIFetchResponseDTO.FinancialInformation> fiList) throws JsonProcessingException {

        List<FIPAccount> fipAccounts = new ArrayList<>();

        for (FIFetchResponseDTO.FinancialInformation fi : fiList) {
            FIPAccount fipAccount = FIPAccount.builder()
                    .financialData(financialData)
                    .fipId(fi.getFipId())
                    .fiType(fi.getFiType())
                    .accountNumber(fi.getAccountNumber())
                    .accountId(fi.getAccountId())
                    .fiDataId(fi.getFiDataId())
                    .accountAnalyticsAvailable(fi.getAccountAnalyticsAvailable())
                    .accountSubAnalyticsAvailable(fi.getAccountSubAnalyticsAvailable())
                    .fiData(fi.getFiData() != null ? objectMapper.writeValueAsString(fi.getFiData()) : null)
                    .build();

            fipAccounts.add(fipAccount);
        }

        fipAccountRepository.saveAll(fipAccounts);

        log.info("Stored {} FIP account records", fipAccounts.size());
    }

    /**
     * Gets financial data for a consent request
     */
    @Transactional(readOnly = true)
    public Mono<FinancialData> getFinancialData(Long consentRequestId) {
        log.info("Retrieving financial data for consent: {}", consentRequestId);

        return Mono.fromCallable(() -> {
            ConsentRequest consentRequest = consentRequestRepository.findById(consentRequestId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Consent request not found: " + consentRequestId));

            return financialDataRepository.findByConsentRequest(consentRequest)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Financial data not found for consent: " + consentRequestId));
        });
    }

    /**
     * Gets FIP accounts for financial data
     */
    @Transactional(readOnly = true)
    public Mono<List<FIPAccount>> getFIPAccounts(Long financialDataId) {
        log.info("Retrieving FIP accounts for financial data: {}", financialDataId);

        return Mono.fromCallable(() ->
                fipAccountRepository.findByFinancialDataId(financialDataId)
        );
    }
}
