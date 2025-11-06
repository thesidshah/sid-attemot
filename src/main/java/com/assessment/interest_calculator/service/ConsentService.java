package com.assessment.interest_calculator.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.assessment.interest_calculator.config.DigioAAProperties;
import com.assessment.interest_calculator.dto.ConsentRequestDTO;
import com.assessment.interest_calculator.dto.ConsentResponseDTO;
import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus;
import com.assessment.interest_calculator.entity.ConsentRequest.NotificationMode;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsentService {
    private final DigioAAClient digioAAClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final DigioAAProperties digioAAProperties;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    /**
     * Initiate a consent request for a potential customer.
     * Creates a consent request with Digio AA and persists it to the database.
     *
     * @param customerName Customer's full name
     * @param customerEmail Customer's email address
     * @param customerMobile Customer's mobile number
     * @param customerRefId Unique reference ID for the customer
     * @param customerIdentifier Customer identifier (should match notification mode - mobile or email)
     * @param fiStartDate Start date for financial information period (format: yyyy-MM-dd HH:mm:ss)
     * @param fiEndDate End date for financial information period (format: yyyy-MM-dd HH:mm:ss)
     * @param consentStartDate Start date for consent validity (format: yyyy-MM-dd HH:mm:ss)
     * @param consentExpiryDate Expiry date for consent (format: yyyy-MM-dd HH:mm:ss)
     * @param notificationMode Mode of notification (SMS or WHATSAPP)
     * @return Mono<ConsentResponseDTO> containing the consent request response
     */
    public Mono<ConsentResponseDTO> initiateConsentRequest(
            String customerName,
            String customerEmail,
            String customerMobile,
            String customerRefId,
            String customerIdentifier,
            String fiStartDate,
            String fiEndDate,
            String consentStartDate,
            String consentExpiryDate,
            NotificationMode notificationMode) {

        log.info("Initiating consent request for customer: {}, refId: {}", customerName, customerRefId);

        // Build customer details
        ConsentRequestDTO.CustomerDetails customerDetails = ConsentRequestDTO.CustomerDetails.builder()
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerMobile(customerMobile)
                .customerRefId(customerRefId)
                .customerIdentifier(customerIdentifier)
                .build();

        // Build consent details
        ConsentRequestDTO.ConsentDetails consentDetails = ConsentRequestDTO.ConsentDetails.builder()
                .fiStartDate(fiStartDate)
                .fiEndDate(fiEndDate)
                .consentStartDate(consentStartDate)
                .consentExpiryDate(consentExpiryDate)
                .build();

        // Build the complete consent request DTO
        ConsentRequestDTO requestDTO = ConsentRequestDTO.builder()
                .customerDetails(customerDetails)
                .consentDetails(consentDetails)
                .templateId(digioAAProperties.getTemplateId())
                .customerId("") // Empty as per the curl example
                .notifyCustomer(true)
                .customerNotificationMode(notificationMode.name())
                .build();

        // Save initial consent request entity to database
        ConsentRequest consentRequest = ConsentRequest.builder()
                .customerRefId(customerRefId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerMobile(customerMobile)
                .customerIdentifier(customerIdentifier)
                .templateId(digioAAProperties.getTemplateId())
                .status(ConsentStatus.PENDING)
                .customerNotificationMode(notificationMode)
                .notifyCustomer(true)
                .fiStartDate(LocalDate.parse(fiStartDate.split(" ")[0], DATE_FORMATTER))
                .fiEndDate(LocalDate.parse(fiEndDate.split(" ")[0], DATE_FORMATTER))
                .consentStartDate(OffsetDateTime.parse(consentStartDate.replace(" ", "T") + "+00:00"))
                .consentExpiryDate(OffsetDateTime.parse(consentExpiryDate.replace(" ", "T") + "+00:00"))
                .build();

        ConsentRequest savedRequest = consentRequestRepository.save(consentRequest);
        log.info("Saved consent request to database with id: {}", savedRequest.getId());

        // Make the API call to Digio AA
        return digioAAClient.makeConsentRequest(requestDTO)
                .doOnSuccess(response -> {
                    // Update the saved entity with response data
                    savedRequest.setConsentHandle(response.getConsentRequestId());
                    savedRequest.setStatus(ConsentStatus.valueOf(response.getStatus().toUpperCase()));
                    consentRequestRepository.save(savedRequest);
                    log.info("Updated consent request with consentHandle: {}", response.getConsentRequestId());
                })
                .doOnError(error -> {
                    log.error("Failed to create consent request for customerRefId: {}", customerRefId, error);
                    // Optionally update status to REJECTED or handle error state
                });
    }

}
