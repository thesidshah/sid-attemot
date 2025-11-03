package com.assessment.interest_calculator.service;

import com.assessment.interest_calculator.config.DigioAAProperties;
import com.assessment.interest_calculator.dto.*;
import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;
import com.assessment.interest_calculator.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AAConsentService {

    private final DigioAAClient digioAAClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final DigioAAProperties properties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Initiates a consent request for a loan account
     */
    @Transactional
    public Mono<ConsentStatusResponse> initiateConsent(InitiateConsentRequest request) {
        log.info("Initiating consent for loan account: {}", request.getLoanAccountId());

        return Mono.fromCallable(() -> {
                    // Validate loan account exists
                    LoanAccount loanAccount = loanAccountRepository.findById(request.getLoanAccountId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Loan account not found: " + request.getLoanAccountId()));

                    // Generate unique customer reference ID
                    String customerRefId = generateCustomerRefId(request);

                    // Calculate consent dates
                    OffsetDateTime now = OffsetDateTime.now();
                    OffsetDateTime consentStart = now;
                    OffsetDateTime consentExpiry = now.plusYears(1);
                    LocalDate fiStartDate = loanAccount.getDateOfDisbursal();
                    LocalDate fiEndDate = LocalDate.now();

                    // Create and save initial consent request entity
                    ConsentRequest consentRequest = ConsentRequest.builder()
                            .loanAccount(loanAccount)
                            .customerRefId(customerRefId)
                            .templateId(properties.getTemplateId())
                            .status(ConsentRequest.ConsentStatus.PENDING)
                            .consentStartDate(consentStart)
                            .consentExpiryDate(consentExpiry)
                            .fiStartDate(fiStartDate)
                            .fiEndDate(fiEndDate)
                            .customerMessage(request.getCustomerMobile())
                            .notifyCustomer(true)
                            .build();

                    // Determine notification mode based on request
                    if (request.getCustomerMobile() != null && !request.getCustomerMobile().isEmpty()) {
                        consentRequest.setCustomerNotificationMode(ConsentRequest.NotificationMode.SMS);
                    }

                    consentRequestRepository.save(consentRequest);

                    return consentRequest;
                })
                .flatMap(consentRequest ->
                        // Call Digio API to create consent
                        digioAAClient.createConsentRequest(buildConsentRequestDTO(
                                        request,
                                        consentRequest.getCustomerRefId(),
                                        consentRequest.getConsentStartDate(),
                                        consentRequest.getConsentExpiryDate(),
                                        consentRequest.getFiStartDate(),
                                        consentRequest.getFiEndDate()
                                ))
                                .flatMap(response -> Mono.fromCallable(() -> {
                                    // Update consent request with Digio response
                                    consentRequest.setConsentRequestId(response.getConsentRequestId());
                                    consentRequest.setGatewayTokenId(response.getGatewayTokenId());

                                    // Build redirect URL
                                    String redirectUrl = buildRedirectUrl(response.getGatewayTokenId());
                                    consentRequest.setRedirectUrl(redirectUrl);

                                    consentRequestRepository.save(consentRequest);

                                    log.info("Consent created successfully - ID: {}, Redirect: {}",
                                            response.getConsentRequestId(), redirectUrl);

                                    return ConsentStatusResponse.builder()
                                            .id(consentRequest.getId())
                                            .digioConsentRequestId(response.getConsentRequestId())
                                            .status(consentRequest.getStatus().name())
                                            .redirectUrl(redirectUrl)
                                            .customerRefId(consentRequest.getCustomerRefId())
                                            .build();
                                }))
                )
                .onErrorResume(error -> {
                    log.error("Error initiating consent: {}", error.getMessage(), error);
                    return Mono.error(new RuntimeException("Failed to initiate consent: " + error.getMessage()));
                });
    }

    /**
     * Gets the current status of a consent request
     */
    @Transactional(readOnly = true)
    public Mono<ConsentStatusResponse> getConsentStatus(Long consentRequestId) {
        log.info("Fetching consent status for ID: {}", consentRequestId);

        return Mono.fromCallable(() ->
            consentRequestRepository.findById(consentRequestId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Consent request not found: " + consentRequestId))
        )
        .flatMap(consentRequest -> {
            if (consentRequest.getConsentRequestId() == null) {
                // Not yet created with Digio
                return Mono.just(buildConsentStatusResponse(consentRequest));
            }

            // Fetch latest status from Digio
            return digioAAClient.getConsentDetails(consentRequest.getConsentRequestId())
                    .flatMap(details -> Mono.fromCallable(() -> {
                        // Update local status
                        updateConsentFromDigioResponse(consentRequest, details);
                        consentRequestRepository.save(consentRequest);

                        return buildConsentStatusResponse(consentRequest);
                    }))
                    .onErrorResume(error -> {
                        log.error("Error fetching consent details from Digio: {}", error.getMessage());
                        // Return current local status on error
                        return Mono.just(buildConsentStatusResponse(consentRequest));
                    });
        });
    }

    private ConsentRequestDTO buildConsentRequestDTO(
            InitiateConsentRequest request,
            String customerRefId,
            OffsetDateTime consentStart,
            OffsetDateTime consentExpiry,
            LocalDate fiStartDate,
            LocalDate fiEndDate) {

        return ConsentRequestDTO.builder()
                .customerDetails(ConsentRequestDTO.CustomerDetails.builder()
                        .customerName(request.getCustomerIdentifier())
                        .customerEmail(request.getCustomerEmail())
                        .customerRefId(customerRefId)
                        .customerMobile(request.getCustomerMobile())
                        .customerIdentifier(request.getCustomerIdentifier())
                        .build())
                .templateId(properties.getTemplateId())
                .consentDetails(ConsentRequestDTO.ConsentDetails.builder()
                        .consentStartDate(consentStart.format(DATETIME_FORMATTER))
                        .consentExpiryDate(consentExpiry.format(DATETIME_FORMATTER))
                        .fiStartDate(fiStartDate.format(DATE_FORMATTER))
                        .fiEndDate(fiEndDate.format(DATE_FORMATTER))
                        .meta(ConsentRequestDTO.ConsentMeta.builder()
                                .emailId(request.getCustomerEmail())
                                .pan(request.getPan())
                                .dob(request.getDob())
                                .showConsentInfo(true)
                                .build())
                        .build())
                .customerNotificationMode("SMS")
                .notifyCustomer(true)
                .build();
    }

    private void updateConsentFromDigioResponse(
            ConsentRequest consentRequest,
            ConsentDetailsResponseDTO details) {

        String status = details.getStatus();

        if ("APPROVED".equalsIgnoreCase(status)) {
            consentRequest.setStatus(ConsentRequest.ConsentStatus.APPROVED);
            consentRequest.setApprovedAt(OffsetDateTime.now());
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            consentRequest.setStatus(ConsentRequest.ConsentStatus.REJECTED);
            consentRequest.setRejectedAt(OffsetDateTime.now());
        } else if ("EXPIRED".equalsIgnoreCase(status)) {
            consentRequest.setStatus(ConsentRequest.ConsentStatus.EXPIRED);
        }
    }

    private ConsentStatusResponse buildConsentStatusResponse(ConsentRequest consentRequest) {
        return ConsentStatusResponse.builder()
                .id(consentRequest.getId())
                .digioConsentRequestId(consentRequest.getConsentRequestId())
                .status(consentRequest.getStatus().name())
                .redirectUrl(consentRequest.getRedirectUrl())
                .customerRefId(consentRequest.getCustomerRefId())
                .approvedAt(consentRequest.getApprovedAt())
                .rejectedAt(consentRequest.getRejectedAt())
                .build();
    }

    private String generateCustomerRefId(InitiateConsentRequest request) {
        return "CUST_" + request.getLoanAccountId() + "_" +
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildRedirectUrl(String gatewayTokenId) {
        return properties.getGatewayBaseUrl() + "/consent/" + gatewayTokenId;
    }
}
