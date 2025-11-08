package com.assessment.interest_calculator.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.assessment.interest_calculator.config.DigioAAProperties;
import com.assessment.interest_calculator.dto.ConsentRequestDTO;
import com.assessment.interest_calculator.dto.ConsentResponseDTO;
import com.assessment.interest_calculator.dto.ConsentStatusResponse;
import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus;
import com.assessment.interest_calculator.entity.ConsentRequest.NotificationMode;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
    private final Validator validator;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");


    /**
     * Initiate a consent request for a potential customer using a ConsentRequestDTO object.
     * This is the preferred method with built-in validation.
     *
     * @param requestDTO ConsentRequestDTO object containing all required details
     * @param notificationMode Mode of notification (SMS / WHATSAPP or EMAIL)
     * @return Mono<ConsentResponseDTO> containing the consent request response
     * @throws IllegalArgumentException if validation fails
     */
    public Mono<ConsentResponseDTO> initiateConsentRequest(
            ConsentRequestDTO requestDTO,
            NotificationMode notificationMode) {

        log.info("Initiating consent request for customer: {}, refId: {}",
                requestDTO.getCustomerDetails().getCustomerName(),
                requestDTO.getCustomerDetails().getCustomerRefId());

        // Validate date formats and business logic
        validateConsentRequest(requestDTO, notificationMode);

        return initiateConsentRequest(
                requestDTO.getCustomerDetails().getCustomerName(),
                requestDTO.getCustomerDetails().getCustomerEmail(),
                requestDTO.getCustomerDetails().getCustomerMobile(),
                requestDTO.getCustomerDetails().getCustomerRefId(),
                requestDTO.getCustomerDetails().getCustomerIdentifier(),
                requestDTO.getConsentDetails().getFiStartDate(),
                requestDTO.getConsentDetails().getFiEndDate(),
                requestDTO.getConsentDetails().getConsentStartDate(),
                requestDTO.getConsentDetails().getConsentExpiryDate(),
                notificationMode
        );
    }

    /**
     * Initiate a consent request for a potential customer.
     * Creates a consent request with Digio AA and persists it to the database.
     *
     * @param customerName Customer's full name
     * @param customerEmail Customer's email address
     * @param customerMobile Customer's mobile number
     * @param customerRefId Unique reference ID for the customer
     * @param customerIdentifier Customer identifier (should match notification mode - mobile or email)
     * @param fiStartDate Start date for financial information period (format: yyyy-MM-dd HH:mm:ss in IST)
     * @param fiEndDate End date for financial information period (format: yyyy-MM-dd HH:mm:ss in IST)
     * @param consentStartDate Start date for consent validity (format: yyyy-MM-dd HH:mm:ss in IST)
     * @param consentExpiryDate Expiry date for consent (format: yyyy-MM-dd HH:mm:ss in IST)
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

        // Parse dates with IST timezone
        LocalDate fiStart = parseDate(fiStartDate);
        LocalDate fiEnd = parseDate(fiEndDate);
        OffsetDateTime consentStart = parseDateTime(consentStartDate);
        OffsetDateTime consentExpiry = parseDateTime(consentExpiryDate);

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
                .fiStartDate(fiStart)
                .fiEndDate(fiEnd)
                .consentStartDate(consentStart)
                .consentExpiryDate(consentExpiry)
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

    /**
     * Validates the consent request for date format and business logic.
     *
     * @param requestDTO The consent request DTO to validate
     * @param notificationMode The notification mode
     * @throws IllegalArgumentException if validation fails
     */
    private void validateConsentRequest(ConsentRequestDTO requestDTO, NotificationMode notificationMode) {
        // Validate using Jakarta Validation annotations
        Set<ConstraintViolation<ConsentRequestDTO>> violations = validator.validate(requestDTO);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errorMessage);
        }

        // Validate date formats and business logic
        try {
            LocalDate fiStart = parseDate(requestDTO.getConsentDetails().getFiStartDate());
            LocalDate fiEnd = parseDate(requestDTO.getConsentDetails().getFiEndDate());
            OffsetDateTime consentStart = parseDateTime(requestDTO.getConsentDetails().getConsentStartDate());
            OffsetDateTime consentExpiry = parseDateTime(requestDTO.getConsentDetails().getConsentExpiryDate());

            // Business logic validations
            if (fiEnd.isBefore(fiStart)) {
                throw new IllegalArgumentException("FI end date must be after FI start date");
            }

            if (consentExpiry.isBefore(consentStart)) {
                throw new IllegalArgumentException("Consent expiry date must be after consent start date");
            }

            // Allow dates from start of today (IST) onwards to accommodate timezone differences
            OffsetDateTime nowIST = OffsetDateTime.now(IST_ZONE);
            if (consentStart.isAfter(nowIST)) {
    throw new IllegalArgumentException("Consent start date cannot be in the future (IST)");
}

            //TODO: This validation can handle more means of communication in future
            // Validate customer identifier matches notification mode
            if (notificationMode == NotificationMode.SMS) {
                if (!requestDTO.getCustomerDetails().getCustomerIdentifier()
                        .equals(requestDTO.getCustomerDetails().getCustomerMobile())) {
                    throw new IllegalArgumentException(
                        "Customer identifier must match mobile number for SMS notification mode");
                }
            }
            else {
                if(!requestDTO.getCustomerDetails().getCustomerIdentifier()
                        .equals(requestDTO.getCustomerDetails().getCustomerEmail())) {
                    throw new IllegalArgumentException(
                        "Customer identifier must match email address for email notification mode");
                }
            }

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss", e);
        }
    }

    /**
     * Parses date string in format "yyyy-MM-dd HH:mm:ss" to LocalDate.
     *
     * @param dateTimeStr Date time string in IST
     * @return LocalDate parsed from the string
     */
    private LocalDate parseDate(String dateTimeStr) {
        String datePart = dateTimeStr.split(" ")[0];
        return LocalDate.parse(datePart, DATE_FORMATTER);
    }

    /**
     * Parses date time string in format "yyyy-MM-dd HH:mm:ss" to OffsetDateTime in IST.
     *
     * @param dateTimeStr Date time string in IST
     * @return OffsetDateTime with IST timezone
     */
    private OffsetDateTime parseDateTime(String dateTimeStr) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        return localDateTime.atZone(IST_ZONE).toOffsetDateTime();
    }

    /**
     * Get the current status of a consent request by customer reference ID.
     * Fetches the latest status from Digio AA and updates the local database.
     *
     * @param customerRefId The customer reference ID
     * @return Mono<ConsentStatusResponse> containing the current consent status
     * @throws IllegalArgumentException if consent request not found
     */
    public Mono<ConsentStatusResponse> getConsentStatusByCustomerRefId(String customerRefId) {
        log.info("Fetching consent status for customerRefId: {}", customerRefId);

        // Find the consent request in database
        ConsentRequest consentRequest = consentRequestRepository.findByCustomerRefId(customerRefId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Consent request not found for customerRefId: " + customerRefId));

        if (consentRequest.getConsentHandle() == null) {
            log.warn("Consent handle not yet available for customerRefId: {}", customerRefId);
            // Return current status from database without calling Digio
            return Mono.just(buildConsentStatusResponse(consentRequest));
        }

        // Fetch latest details from Digio AA
        return digioAAClient.getConsentDetails(consentRequest.getConsentHandle())
                .map(consentDetails -> {
                    // Update the consent request entity with latest details from Digio
                    updateConsentRequestFromDetails(consentRequest, consentDetails);
                    consentRequestRepository.save(consentRequest);

                    log.info("Updated consent status for customerRefId: {} to status: {}",
                            customerRefId, consentRequest.getStatus());

                    return buildConsentStatusResponse(consentRequest);
                })
                .onErrorResume(error -> {
                    log.error("Error fetching consent details from Digio AA for customerRefId: {}",
                            customerRefId, error);
                    // Return current status from database on error
                    return Mono.just(buildConsentStatusResponse(consentRequest));
                });
    }

    /**
     * Get the current status of a consent request by consent handle.
     * Fetches the latest status from Digio AA and updates the local database.
     *
     * @param consentHandle The consent handle from Digio AA
     * @return Mono<ConsentStatusResponse> containing the current consent status
     * @throws IllegalArgumentException if consent request not found
     */
    public Mono<ConsentStatusResponse> getConsentStatusByHandle(String consentHandle) {
        log.info("Fetching consent status for consentHandle: {}", consentHandle);

        // Find the consent request in database
        ConsentRequest consentRequest = consentRequestRepository.findByConsentHandle(consentHandle)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Consent request not found for consentHandle: " + consentHandle));

        // Fetch latest details from Digio AA
        return digioAAClient.getConsentDetails(consentHandle)
                .map(consentDetails -> {
                    // Update the consent request entity with latest details from Digio
                    updateConsentRequestFromDetails(consentRequest, consentDetails);
                    consentRequestRepository.save(consentRequest);

                    log.info("Updated consent status for consentHandle: {} to status: {}",
                            consentHandle, consentRequest.getStatus());

                    return buildConsentStatusResponse(consentRequest);
                })
                .onErrorResume(error -> {
                    log.error("Error fetching consent details from Digio AA for consentHandle: {}",
                            consentHandle, error);
                    // Return current status from database on error
                    return Mono.just(buildConsentStatusResponse(consentRequest));
                });
    }

    /**
     * Updates the ConsentRequest entity with details from Digio AA response.
     *
     * @param consentRequest The entity to update
     * @param consentDetails The details from Digio AA
     */
    private void updateConsentRequestFromDetails(
            ConsentRequest consentRequest,
            ConsentResponseDTO.ConsentDetails consentDetails) {

        // Parse and update dates if available
        if (consentDetails.getConsentStartDate() != null) {
            consentRequest.setConsentStartDate(parseDateTime(consentDetails.getConsentStartDate()));
        }
        if (consentDetails.getConsentExpiryDate() != null) {
            consentRequest.setConsentExpiryDate(parseDateTime(consentDetails.getConsentExpiryDate()));
        }
        if (consentDetails.getFiStartDate() != null) {
            consentRequest.setFiStartDate(parseDate(consentDetails.getFiStartDate()));
        }
        if (consentDetails.getFiEndDate() != null) {
            consentRequest.setFiEndDate(parseDate(consentDetails.getFiEndDate()));
        }

        // Note: Status update would need to come from the full consent response
        // For now, we're only updating dates. Status updates typically come from webhooks
    }

    /**
     * Builds a ConsentStatusResponse from a ConsentRequest entity.
     *
     * @param consentRequest The consent request entity
     * @return ConsentStatusResponse DTO
     */
    private ConsentStatusResponse buildConsentStatusResponse(ConsentRequest consentRequest) {
        return ConsentStatusResponse.builder()
                .consentHandle(consentRequest.getConsentHandle())
                .status(consentRequest.getStatus().name())
                .redirectUrl(consentRequest.getRedirectUrl())
                .createdAt(consentRequest.getCreatedAt())
                .expiresAt(consentRequest.getConsentExpiryDate())
                .message(buildStatusMessage(consentRequest.getStatus()))
                .build();
    }

    /**
     * Builds a user-friendly status message based on consent status.
     *
     * @param status The consent status
     * @return User-friendly message
     */
    private String buildStatusMessage(ConsentStatus status) {
        return switch (status) {
            case PENDING -> "Consent request is pending customer approval";
            case APPROVED -> "Consent has been approved by customer";
            case REJECTED -> "Consent request was rejected by customer";
            case EXPIRED -> "Consent request has expired";
            case DATA_FETCHED -> "Financial information has been fetched";
        };
    }

}
