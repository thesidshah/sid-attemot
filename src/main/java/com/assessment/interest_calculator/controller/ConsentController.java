package com.assessment.interest_calculator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.assessment.interest_calculator.dto.ConsentRequestDTO;
import com.assessment.interest_calculator.dto.ConsentResponseDTO;
import com.assessment.interest_calculator.dto.ConsentStatusResponse;
import com.assessment.interest_calculator.entity.ConsentRequest.NotificationMode;
import com.assessment.interest_calculator.service.ConsentService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/consent")
@Slf4j
public class ConsentController {
    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    /**
     * Initiate a new consent request for account aggregator.
     *
     * @param requestDTO The consent request payload
     * @param notificationMode The notification mode (SMS or EMAIL)
     * @return ResponseEntity containing the consent response
     */
    @PostMapping("/request")
    public ResponseEntity<ConsentResponseDTO> initiateConsentRequest(
            @Valid @RequestBody ConsentRequestDTO requestDTO,
            @RequestParam(defaultValue = "SMS") NotificationMode notificationMode) {

        log.info("Received consent request for customer: {}, notificationMode: {}",
                requestDTO.getCustomerDetails().getCustomerRefId(), notificationMode);

        ConsentResponseDTO response = consentService
                .initiateConsentRequest(requestDTO, notificationMode)
                .block();

        log.info("Consent request initiated successfully. Handle: {}",
                response != null ? response.getConsentRequestId() : "null");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get consent status by customer reference ID.
     *
     * @param customerRefId The customer reference ID
     * @return ResponseEntity containing the consent status
     */
    @GetMapping("/status/customer/{customerRefId}")
    public ResponseEntity<ConsentStatusResponse> getConsentStatusByCustomerRefId(
            @PathVariable String customerRefId) {

        log.info("Fetching consent status for customerRefId: {}", customerRefId);

        ConsentStatusResponse response = consentService
                .getConsentStatusByCustomerRefId(customerRefId)
                .block();

        log.info("Retrieved consent status for customerRefId: {}, status: {}",
                customerRefId, response != null ? response.getStatus() : "null");

        return ResponseEntity.ok(response);
    }

    /**
     * Get consent status by consent handle.
     *
     * @param consentHandle The consent handle from Digio AA
     * @return ResponseEntity containing the consent status
     */
    @GetMapping("/status/handle/{consentHandle}")
    public ResponseEntity<ConsentStatusResponse> getConsentStatusByHandle(
            @PathVariable String consentHandle) {

        log.info("Fetching consent status for consentHandle: {}", consentHandle);

        ConsentStatusResponse response = consentService
                .getConsentStatusByHandle(consentHandle)
                .block();

        log.info("Retrieved consent status for consentHandle: {}, status: {}",
                consentHandle, response != null ? response.getStatus() : "null");

        return ResponseEntity.ok(response);
    }
}
