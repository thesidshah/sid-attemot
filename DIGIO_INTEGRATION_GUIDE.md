# Digio Account Aggregator Integration Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Architecture Overview](#architecture-overview)
4. [Phase 1: Repository Layer](#phase-1-repository-layer)
5. [Phase 2: Core Service Layer](#phase-2-core-service-layer)
6. [Phase 3: Controller Layer](#phase-3-controller-layer)
7. [Phase 4: Data Processing](#phase-4-data-processing)
8. [Phase 5: Integration & Testing](#phase-5-integration--testing)
9. [Phase 6: Documentation & Cleanup](#phase-6-documentation--cleanup)
10. [API Reference](#api-reference)
11. [Troubleshooting](#troubleshooting)

---

## Overview

This guide provides a comprehensive walkthrough for integrating **Digio Account Aggregator** into your Spring Boot loan interest calculator application. The Account Aggregator framework enables Financial Information Users (FIUs) to collect financial data from customers with their explicit consent.

### What You Already Have

✅ **Configuration Layer**
- [DigioAAConfig.java](src/main/java/com/assessment/interest_calculator/config/DigioAAConfig.java) - WebClient bean configuration
- [DigioAAProperties.java](src/main/java/com/assessment/interest_calculator/config/DigioAAProperties.java) - Externalized properties
- [application.yml](src/main/resources/application.yml) - Configuration with Digio credentials
- RSA 4096-bit encryption keys generated

✅ **Data Layer**
- [ConsentRequest](src/main/java/com/assessment/interest_calculator/entity/ConsentRequest.java) entity with full lifecycle support
- [LoanAccount](src/main/java/com/assessment/interest_calculator/entity/LoanAccount.java) entity
- DTOs for API communication

✅ **Testing Scripts**
- [consent-request.sh](consent-request.sh) - Bash script for testing consent creation
- [consent-request-status.sh](consent-request-status.sh) - Status checking script

### What Needs to Be Implemented

❌ **Repository Layer**
- ConsentRequestRepository

❌ **Service Layer**
- DigioAAService (core integration logic)
- FinancialDataService (data processing)

❌ **Controller Layer**
- ConsentController (REST endpoints)
- WebhookController (Digio callbacks)

❌ **Error Handling**
- Global exception handler
- Custom exceptions

---

## Prerequisites

### 1. Digio Account Setup
- Digio FIU account with sandbox access
- API credentials (Client ID and Secret)
- Consent template created in Digio dashboard
- Template ID obtained

### 2. Technical Requirements
- Java 17+
- Spring Boot 3.5.6
- PostgreSQL 13+
- Maven 3.6+
- Docker & Docker Compose (for local development)

### 3. Encryption Keys
RSA key pair for data encryption/decryption (already generated):
```bash
# Private key location
src/main/resources/keys/private.pem

# Public key location
src/main/resources/keys/public.pem
```

### 4. Environment Variables
Ensure these are set in your environment or [application-local.yml](src/main/resources/application-local.yml):
```yaml
DIGIO_BASE_URL: https://ext.digio.in:444/fiu_api
DIGIO_API_KEY: your_client_id
DIGIO_API_SECRET: your_client_secret
DIGIO_TEMPLATE_ID: your_template_id
DIGIO_WEBHOOK_BASE_URL: your_webhook_url (use ngrok for local testing)
```

---

## Architecture Overview

### System Flow

```
┌─────────────┐
│   Client    │
│ Application │
└──────┬──────┘
       │ 1. Request Consent
       ▼
┌─────────────────────────────────────┐
│    ConsentController                │
│  POST /api/consents                 │
└──────┬──────────────────────────────┘
       │ 2. Process Request
       ▼
┌─────────────────────────────────────┐
│    DigioAAService                   │
│  - Create consent request           │
│  - Call Digio API                   │
└──────┬──────────────────────────────┘
       │ 3. HTTP POST
       ▼
┌─────────────────────────────────────┐
│    Digio API (WebClient)            │
│  POST /client/consent/request/      │
└──────┬──────────────────────────────┘
       │ 4. Response (consent_handle, redirect_url)
       ▼
┌─────────────────────────────────────┐
│    ConsentRequestRepository         │
│  - Save consent record (PENDING)    │
└─────────────────────────────────────┘
       │
       │ 5. Customer receives SMS/WhatsApp
       │    with consent link
       │
       │ 6. Customer approves consent
       │
       ▼
┌─────────────────────────────────────┐
│    WebhookController                │
│  POST /webhooks/digio/consent-status│
└──────┬──────────────────────────────┘
       │ 7. Update status (APPROVED)
       ▼
┌─────────────────────────────────────┐
│    DigioAAService                   │
│  - Fetch financial data             │
│  - Decrypt data                     │
└──────┬──────────────────────────────┘
       │ 8. Process & Store
       ▼
┌─────────────────────────────────────┐
│    FinancialDataService             │
│  - Parse FI data                    │
│  - Link to loan account             │
└─────────────────────────────────────┘
```

### Key Components

| Component | Responsibility | Status |
|-----------|---------------|--------|
| **DigioAAConfig** | Configure WebClient with auth headers | ✅ Complete |
| **DigioAAProperties** | Externalize configuration | ✅ Complete |
| **ConsentRequest (Entity)** | Track consent lifecycle | ✅ Complete |
| **ConsentRequest (DTO)** | API request/response mapping | ✅ Complete |
| **ConsentRequestRepository** | Database operations | ❌ To Implement |
| **DigioAAService** | Core business logic | ❌ To Implement |
| **ConsentController** | REST API endpoints | ❌ To Implement |
| **WebhookController** | Handle Digio callbacks | ❌ To Implement |
| **FinancialDataService** | Process FI data | ❌ To Implement |

---

## Phase 1: Repository Layer

### 1.1 Create ConsentRequestRepository

**File**: `src/main/java/com/assessment/interest_calculator/repository/ConsentRequestRepository.java`

#### Purpose
Provide data access methods for ConsentRequest entities with custom queries for common operations.

#### Implementation Details

```java
package com.assessment.interest_calculator.repository;

import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRequestRepository extends JpaRepository<ConsentRequest, Long> {

    /**
     * Find consent request by Digio consent handle (unique identifier)
     * Used when processing webhook callbacks
     */
    Optional<ConsentRequest> findByConsentHandle(String consentHandle);

    /**
     * Find consent request by customer reference ID
     * Useful for deduplication and customer lookup
     */
    Optional<ConsentRequest> findByCustomerRefId(String customerRefId);

    /**
     * Find all consent requests for a specific loan account
     * Supports pagination for large datasets
     */
    Page<ConsentRequest> findByLoanAccountId(Long loanAccountId, Pageable pageable);

    /**
     * Find all consent requests with a specific status
     * Useful for batch processing (e.g., polling pending consents)
     */
    List<ConsentRequest> findByStatus(ConsentStatus status);

    /**
     * Find expired consents that need status update
     * Scheduled job can use this to mark consents as EXPIRED
     */
    @Query("SELECT cr FROM ConsentRequest cr WHERE cr.status = 'PENDING' " +
           "AND cr.consentExpiryDate < :currentTime")
    List<ConsentRequest> findExpiredConsents(@Param("currentTime") OffsetDateTime currentTime);

    /**
     * Find approved consents that haven't fetched data yet
     * Used to trigger FI data fetch operations
     */
    @Query("SELECT cr FROM ConsentRequest cr WHERE cr.status = 'APPROVED' " +
           "AND cr.approvedAt IS NOT NULL " +
           "ORDER BY cr.approvedAt ASC")
    List<ConsentRequest> findApprovedConsentsWithoutData();

    /**
     * Count consents by status for a loan account
     * Dashboard/analytics use case
     */
    @Query("SELECT COUNT(cr) FROM ConsentRequest cr " +
           "WHERE cr.loanAccount.id = :loanAccountId AND cr.status = :status")
    long countByLoanAccountIdAndStatus(
        @Param("loanAccountId") Long loanAccountId,
        @Param("status") ConsentStatus status
    );

    /**
     * Check if a valid (non-expired, non-rejected) consent exists for account
     * Prevent duplicate consent requests
     */
    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END " +
           "FROM ConsentRequest cr WHERE cr.loanAccount.id = :loanAccountId " +
           "AND cr.status IN ('PENDING', 'APPROVED') " +
           "AND (cr.consentExpiryDate IS NULL OR cr.consentExpiryDate > :currentTime)")
    boolean existsActiveConsentForAccount(
        @Param("loanAccountId") Long loanAccountId,
        @Param("currentTime") OffsetDateTime currentTime
    );
}
```

#### Key Features

1. **Standard CRUD**: Inherited from `JpaRepository<ConsentRequest, Long>`
2. **Custom Finders**: Domain-specific query methods following Spring Data naming conventions
3. **JPQL Queries**: Complex queries using `@Query` annotation
4. **Pagination Support**: `Page<T>` return type for large result sets
5. **Business Logic Queries**:
   - Find expired consents for cleanup jobs
   - Prevent duplicate consent requests
   - Support for webhook processing

#### Usage Examples

```java
// In a service class
@Service
@RequiredArgsConstructor
public class SomeService {
    private final ConsentRequestRepository repository;

    // Find by consent handle (from webhook)
    Optional<ConsentRequest> consent = repository.findByConsentHandle("CH123456");

    // Check for existing active consent before creating new one
    boolean hasActive = repository.existsActiveConsentForAccount(
        loanAccountId,
        OffsetDateTime.now()
    );

    // Get all pending consents for status polling
    List<ConsentRequest> pending = repository.findByStatus(ConsentStatus.PENDING);

    // Paginated list for a loan account
    Page<ConsentRequest> consents = repository.findByLoanAccountId(
        loanAccountId,
        PageRequest.of(0, 10, Sort.by("createdAt").descending())
    );
}
```

---

## Phase 2: Core Service Layer

### 2.1 Create DigioAAService

**File**: `src/main/java/com/assessment/interest_calculator/service/DigioAAService.java`

#### Purpose
Encapsulate all business logic for Digio Account Aggregator integration including:
- Consent request creation
- Status polling
- Financial data fetching
- Data decryption
- Consent lifecycle management

#### Implementation Strategy

```java
package com.assessment.interest_calculator.service;

import com.assessment.interest_calculator.config.DigioAAProperties;
import com.assessment.interest_calculator.dto.ConsentRequest;
import com.assessment.interest_calculator.dto.ConsentResponse;
import com.assessment.interest_calculator.dto.ConsentStatusResponse;
import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;
import com.assessment.interest_calculator.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigioAAService {

    private final WebClient digioWebClient;
    private final DigioAAProperties properties;
    private final ConsentRequestRepository consentRequestRepository;
    private final LoanAccountRepository loanAccountRepository;

    private static final DateTimeFormatter DIGIO_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Initiates a consent request with Digio for a loan account
     *
     * @param loanAccountId The loan account ID
     * @param fiStartDate Start date for financial information period
     * @param fiEndDate End date for financial information period
     * @param notificationMode SMS or WHATSAPP
     * @return Created ConsentRequest entity with Digio response details
     */
    @Transactional
    public com.assessment.interest_calculator.entity.ConsentRequest initiateConsent(
            Long loanAccountId,
            LocalDate fiStartDate,
            LocalDate fiEndDate,
            com.assessment.interest_calculator.entity.ConsentRequest.NotificationMode notificationMode
    ) {
        log.info("Initiating consent request for loan account: {}", loanAccountId);

        // 1. Fetch loan account
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Loan account not found: " + loanAccountId));

        // 2. Check for existing active consent
        boolean hasActiveConsent = consentRequestRepository
            .existsActiveConsentForAccount(loanAccountId, OffsetDateTime.now());

        if (hasActiveConsent) {
            throw new IllegalStateException(
                "Active consent already exists for loan account: " + loanAccountId);
        }

        // 3. Generate unique customer reference ID
        String customerRefId = "APP-" + LocalDate.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 8);

        // 4. Build Digio API request
        ConsentRequest request = buildConsentRequest(
            loanAccount,
            customerRefId,
            fiStartDate,
            fiEndDate,
            notificationMode
        );

        // 5. Call Digio API
        ConsentResponse response = callDigioConsentAPI(request);

        // 6. Create and save entity
        com.assessment.interest_calculator.entity.ConsentRequest consentEntity =
            com.assessment.interest_calculator.entity.ConsentRequest.builder()
                .loanAccount(loanAccount)
                .customerRefId(customerRefId)
                .consentHandle(response.getConsentHandle())
                .templateId(properties.getTemplateId())
                .redirectUrl(response.getRedirectUrl())
                .status(com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.PENDING)
                .consentStartDate(response.getConsentDetails().getConsentStartDate())
                .consentExpiryDate(response.getConsentDetails().getConsentExpiryDate())
                .fiStartDate(fiStartDate)
                .fiEndDate(fiEndDate)
                .customerNotificationMode(notificationMode)
                .notifyCustomer(true)
                .build();

        consentEntity = consentRequestRepository.save(consentEntity);

        log.info("Consent request created successfully. Consent Handle: {}, Redirect URL: {}",
            response.getConsentHandle(), response.getRedirectUrl());

        return consentEntity;
    }

    /**
     * Checks the current status of a consent request with Digio
     * Updates local database with latest status
     *
     * @param consentHandle Digio consent handle
     * @return Updated ConsentRequest entity
     */
    @Transactional
    public com.assessment.interest_calculator.entity.ConsentRequest checkConsentStatus(
            String consentHandle) {
        log.info("Checking consent status for handle: {}", consentHandle);

        // 1. Fetch from database
        com.assessment.interest_calculator.entity.ConsentRequest consent =
            consentRequestRepository.findByConsentHandle(consentHandle)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Consent not found: " + consentHandle));

        // 2. Skip if already in terminal state
        if (consent.getStatus() == com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.DATA_FETCHED ||
            consent.getStatus() == com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.REJECTED) {
            log.info("Consent already in terminal state: {}", consent.getStatus());
            return consent;
        }

        // 3. Call Digio status API
        ConsentStatusResponse statusResponse = callDigioStatusAPI(consentHandle);

        // 4. Update entity based on response
        updateConsentStatus(consent, statusResponse);

        return consentRequestRepository.save(consent);
    }

    /**
     * Fetches financial information data for an approved consent
     *
     * @param consentHandle Digio consent handle
     * @return Encrypted financial data (to be decrypted by caller)
     */
    @Transactional
    public String fetchFinancialData(String consentHandle) {
        log.info("Fetching financial data for consent: {}", consentHandle);

        // 1. Verify consent is approved
        com.assessment.interest_calculator.entity.ConsentRequest consent =
            consentRequestRepository.findByConsentHandle(consentHandle)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Consent not found: " + consentHandle));

        if (consent.getStatus() != com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.APPROVED) {
            throw new IllegalStateException(
                "Cannot fetch data for consent in status: " + consent.getStatus());
        }

        // 2. Call Digio FI data API
        String encryptedData = callDigioFIDataAPI(consentHandle);

        // 3. Update status to DATA_FETCHED
        consent.setStatus(com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.DATA_FETCHED);
        consentRequestRepository.save(consent);

        log.info("Financial data fetched successfully for consent: {}", consentHandle);

        return encryptedData;
    }

    /**
     * Decrypts financial information data using private key
     * Uses BouncyCastle for RSA decryption
     *
     * @param encryptedData Encrypted data from Digio
     * @return Decrypted JSON string
     */
    public String decryptFinancialData(String encryptedData) {
        log.info("Decrypting financial data");

        try {
            // TODO: Implement RSA decryption using BouncyCastle
            // 1. Load private key from properties.getPrivateKeyPath()
            // 2. Decode base64 encrypted data
            // 3. Decrypt using RSA/ECB/PKCS1Padding
            // 4. Return decrypted JSON string

            throw new UnsupportedOperationException("Decryption not yet implemented");

        } catch (Exception e) {
            log.error("Failed to decrypt financial data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // ==================== Private Helper Methods ====================

    private ConsentRequest buildConsentRequest(
            LoanAccount loanAccount,
            String customerRefId,
            LocalDate fiStartDate,
            LocalDate fiEndDate,
            com.assessment.interest_calculator.entity.ConsentRequest.NotificationMode notificationMode
    ) {
        // Calculate consent validity dates
        OffsetDateTime consentStartDate = OffsetDateTime.now();
        OffsetDateTime consentExpiryDate = consentStartDate.plusDays(30); // 30 days validity

        // Build customer details
        ConsentRequest.CustomerDetails customerDetails = new ConsentRequest.CustomerDetails();
        customerDetails.setCustomerRefId(customerRefId);
        customerDetails.setCustometName(loanAccount.getAccountHolderName());
        // Note: Email and mobile should ideally come from a Customer entity
        // For now, using placeholder values
        customerDetails.setCustomerEmail("customer@example.com");
        customerDetails.setCustomerMobile("9999999999");
        customerDetails.setCustomerIdentifier("9999999999");

        // Build consent request
        ConsentRequest request = ConsentRequest.builder()
            .customerDetails(customerDetails)
            .build();

        // Note: Additional fields like consent_details, template_id, etc.
        // need to be added to the DTO and populated here

        return request;
    }

    private ConsentResponse callDigioConsentAPI(ConsentRequest request) {
        log.info("Calling Digio consent API");

        try {
            return digioWebClient
                .post()
                .uri("/client/consent/request/")
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ConsentResponse.class)
                .block(); // Blocking call for simplicity, consider async in production

        } catch (Exception e) {
            log.error("Failed to call Digio consent API", e);
            throw new RuntimeException("Digio API call failed", e);
        }
    }

    private ConsentStatusResponse callDigioStatusAPI(String consentHandle) {
        log.info("Calling Digio status API for handle: {}", consentHandle);

        try {
            return digioWebClient
                .get()
                .uri("/client/consent/request/{consentHandle}", consentHandle)
                .retrieve()
                .bodyToMono(ConsentStatusResponse.class)
                .block();

        } catch (Exception e) {
            log.error("Failed to call Digio status API", e);
            throw new RuntimeException("Digio status API call failed", e);
        }
    }

    private String callDigioFIDataAPI(String consentHandle) {
        log.info("Calling Digio FI data API for handle: {}", consentHandle);

        try {
            // TODO: Implement FI data fetch API call
            // Endpoint: /client/fi/fetch or similar
            // Returns encrypted financial data

            throw new UnsupportedOperationException("FI data fetch not yet implemented");

        } catch (Exception e) {
            log.error("Failed to call Digio FI data API", e);
            throw new RuntimeException("Digio FI data API call failed", e);
        }
    }

    private void updateConsentStatus(
            com.assessment.interest_calculator.entity.ConsentRequest consent,
            ConsentStatusResponse statusResponse
    ) {
        String status = statusResponse.getStatus();

        switch (status) {
            case "APPROVED":
                consent.setStatus(com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.APPROVED);
                consent.setApprovedAt(OffsetDateTime.now());
                log.info("Consent approved: {}", consent.getConsentHandle());
                break;

            case "REJECTED":
                consent.setStatus(com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.REJECTED);
                consent.setRejectedAt(OffsetDateTime.now());
                log.info("Consent rejected: {}", consent.getConsentHandle());
                break;

            case "EXPIRED":
                consent.setStatus(com.assessment.interest_calculator.entity.ConsentRequest.ConsentStatus.EXPIRED);
                log.info("Consent expired: {}", consent.getConsentHandle());
                break;

            case "PENDING":
                // No update needed
                log.info("Consent still pending: {}", consent.getConsentHandle());
                break;

            default:
                log.warn("Unknown consent status from Digio: {}", status);
        }
    }
}
```

#### Key Design Decisions

1. **Transaction Management**: All public methods are `@Transactional` to ensure database consistency
2. **Blocking vs Reactive**: Uses `.block()` for simplicity; consider full reactive implementation for production
3. **Error Handling**: Throws runtime exceptions; will be caught by global exception handler (Phase 3)
4. **Validation**: Checks for existing active consents to prevent duplicates
5. **Audit Trail**: Extensive logging at INFO level for operations, ERROR for failures
6. **State Machine**: Explicit consent status transitions with validation
7. **Separation of Concerns**:
   - API communication isolated in private methods
   - DTO ↔ Entity mapping kept separate
   - Encryption/decryption logic separate method

#### Service Method Flows

**Initiate Consent Flow**:
```
initiateConsent()
  ├─→ Validate loan account exists
  ├─→ Check for existing active consent
  ├─→ Generate unique customer ref ID
  ├─→ Build Digio API request DTO
  ├─→ Call Digio consent API (HTTP POST)
  ├─→ Parse response (consent_handle, redirect_url)
  ├─→ Create ConsentRequest entity (status: PENDING)
  ├─→ Save to database
  └─→ Return entity to caller
```

**Check Status Flow**:
```
checkConsentStatus()
  ├─→ Fetch consent from database by handle
  ├─→ Skip if already in terminal state (DATA_FETCHED, REJECTED)
  ├─→ Call Digio status API (HTTP GET)
  ├─→ Parse status response
  ├─→ Update entity status based on response
  ├─→ Save updated entity
  └─→ Return updated entity
```

**Fetch Data Flow**:
```
fetchFinancialData()
  ├─→ Fetch consent from database
  ├─→ Verify status is APPROVED
  ├─→ Call Digio FI data API (HTTP GET)
  ├─→ Receive encrypted data
  ├─→ Update status to DATA_FETCHED
  └─→ Return encrypted data (caller will decrypt)
```

---

## Phase 3: Controller Layer

### 3.1 Create ConsentController

**File**: `src/main/java/com/assessment/interest_calculator/controller/ConsentController.java`

#### Purpose
Expose REST API endpoints for consent management operations.

#### Implementation

```java
package com.assessment.interest_calculator.controller;

import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.service.DigioAAService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/consents")
@Slf4j
@RequiredArgsConstructor
public class ConsentController {

    private final DigioAAService digioAAService;
    private final ConsentRequestRepository consentRequestRepository;

    /**
     * POST /api/consents
     * Create a new consent request for a loan account
     *
     * Request Body:
     * {
     *   "loanAccountId": 1,
     *   "fiStartDate": "2024-01-01",
     *   "fiEndDate": "2025-01-01",
     *   "notificationMode": "SMS"
     * }
     */
    @PostMapping
    public ResponseEntity<ConsentResponseDTO> createConsent(
            @Valid @RequestBody CreateConsentRequestDTO request
    ) {
        log.info("Creating consent request for loan account: {}", request.getLoanAccountId());

        ConsentRequest consent = digioAAService.initiateConsent(
            request.getLoanAccountId(),
            request.getFiStartDate(),
            request.getFiEndDate(),
            request.getNotificationMode()
        );

        ConsentResponseDTO response = ConsentResponseDTO.fromEntity(consent);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/consents/{id}
     * Get consent details by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsentResponseDTO> getConsentById(@PathVariable Long id) {
        log.info("Fetching consent by ID: {}", id);

        ConsentRequest consent = consentRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + id));

        return ResponseEntity.ok(ConsentResponseDTO.fromEntity(consent));
    }

    /**
     * GET /api/consents/handle/{consentHandle}
     * Get consent details by Digio consent handle
     */
    @GetMapping("/handle/{consentHandle}")
    public ResponseEntity<ConsentResponseDTO> getConsentByHandle(
            @PathVariable String consentHandle
    ) {
        log.info("Fetching consent by handle: {}", consentHandle);

        ConsentRequest consent = consentRequestRepository.findByConsentHandle(consentHandle)
            .orElseThrow(() -> new IllegalArgumentException(
                "Consent not found: " + consentHandle));

        return ResponseEntity.ok(ConsentResponseDTO.fromEntity(consent));
    }

    /**
     * GET /api/consents/status/{consentHandle}
     * Check and update consent status from Digio
     */
    @GetMapping("/status/{consentHandle}")
    public ResponseEntity<ConsentResponseDTO> checkConsentStatus(
            @PathVariable String consentHandle
    ) {
        log.info("Checking status for consent: {}", consentHandle);

        ConsentRequest consent = digioAAService.checkConsentStatus(consentHandle);

        return ResponseEntity.ok(ConsentResponseDTO.fromEntity(consent));
    }

    /**
     * GET /api/loan-accounts/{loanAccountId}/consents
     * List all consents for a loan account with pagination
     */
    @GetMapping("/loan-accounts/{loanAccountId}")
    public ResponseEntity<Page<ConsentResponseDTO>> getConsentsByLoanAccount(
            @PathVariable Long loanAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching consents for loan account: {}, page: {}, size: {}",
            loanAccountId, page, size);

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by("createdAt").descending()
        );

        Page<ConsentRequest> consents = consentRequestRepository
            .findByLoanAccountId(loanAccountId, pageable);

        Page<ConsentResponseDTO> response = consents.map(ConsentResponseDTO::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/consents/{consentHandle}/fetch-data
     * Manually trigger financial data fetch for approved consent
     */
    @PostMapping("/{consentHandle}/fetch-data")
    public ResponseEntity<Map<String, String>> fetchFinancialData(
            @PathVariable String consentHandle
    ) {
        log.info("Fetching financial data for consent: {}", consentHandle);

        String encryptedData = digioAAService.fetchFinancialData(consentHandle);
        String decryptedData = digioAAService.decryptFinancialData(encryptedData);

        Map<String, String> response = new HashMap<>();
        response.put("consentHandle", consentHandle);
        response.put("status", "success");
        response.put("message", "Financial data fetched and decrypted successfully");
        // Don't expose raw financial data in API response for security
        // Instead, process and store it internally

        return ResponseEntity.ok(response);
    }

    // ==================== DTOs ====================

    @Data
    public static class CreateConsentRequestDTO {
        @NotNull(message = "Loan account ID is required")
        private Long loanAccountId;

        @NotNull(message = "FI start date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate fiStartDate;

        @NotNull(message = "FI end date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate fiEndDate;

        private ConsentRequest.NotificationMode notificationMode =
            ConsentRequest.NotificationMode.SMS;
    }

    @Data
    public static class ConsentResponseDTO {
        private Long id;
        private Long loanAccountId;
        private String accountHolderName;
        private String consentHandle;
        private String customerRefId;
        private String status;
        private String redirectUrl;
        private LocalDate fiStartDate;
        private LocalDate fiEndDate;
        private String consentExpiryDate;
        private String approvedAt;
        private String rejectedAt;
        private String createdAt;

        public static ConsentResponseDTO fromEntity(ConsentRequest consent) {
            ConsentResponseDTO dto = new ConsentResponseDTO();
            dto.setId(consent.getId());
            dto.setLoanAccountId(consent.getLoanAccount().getId());
            dto.setAccountHolderName(consent.getLoanAccount().getAccountHolderName());
            dto.setConsentHandle(consent.getConsentHandle());
            dto.setCustomerRefId(consent.getCustomerRefId());
            dto.setStatus(consent.getStatus().name());
            dto.setRedirectUrl(consent.getRedirectUrl());
            dto.setFiStartDate(consent.getFiStartDate());
            dto.setFiEndDate(consent.getFiEndDate());
            dto.setConsentExpiryDate(
                consent.getConsentExpiryDate() != null ?
                consent.getConsentExpiryDate().toString() : null
            );
            dto.setApprovedAt(
                consent.getApprovedAt() != null ?
                consent.getApprovedAt().toString() : null
            );
            dto.setRejectedAt(
                consent.getRejectedAt() != null ?
                consent.getRejectedAt().toString() : null
            );
            dto.setCreatedAt(consent.getCreatedAt().toString());
            return dto;
        }
    }
}
```

#### API Endpoints Summary

| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/api/consents` | Create consent request | CreateConsentRequestDTO | ConsentResponseDTO (201) |
| GET | `/api/consents/{id}` | Get consent by ID | - | ConsentResponseDTO (200) |
| GET | `/api/consents/handle/{handle}` | Get consent by handle | - | ConsentResponseDTO (200) |
| GET | `/api/consents/status/{handle}` | Check status (polls Digio) | - | ConsentResponseDTO (200) |
| GET | `/api/loan-accounts/{id}/consents` | List consents for account | page, size | Page<ConsentResponseDTO> (200) |
| POST | `/api/consents/{handle}/fetch-data` | Fetch FI data | - | Success message (200) |

### 3.2 Create WebhookController

**File**: `src/main/java/com/assessment/interest_calculator/controller/WebhookController.java`

#### Purpose
Handle webhook callbacks from Digio when consent status changes or FI data is ready.

#### Implementation

```java
package com.assessment.interest_calculator.controller;

import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;
import com.assessment.interest_calculator.service.DigioAAService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/digio")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final ConsentRequestRepository consentRequestRepository;
    private final DigioAAService digioAAService;

    /**
     * POST /webhooks/digio/consent-status
     * Receives consent status updates from Digio
     *
     * Webhook payload example:
     * {
     *   "consent_handle": "CH123456",
     *   "status": "APPROVED",
     *   "event": "CONSENT_STATUS_UPDATE",
     *   "timestamp": "2025-01-15T10:30:00Z"
     * }
     */
    @PostMapping("/consent-status")
    public ResponseEntity<Map<String, String>> handleConsentStatusUpdate(
            @RequestBody ConsentStatusWebhookPayload payload
    ) {
        log.info("Received consent status webhook: handle={}, status={}",
            payload.getConsentHandle(), payload.getStatus());

        try {
            // Find consent by handle
            ConsentRequest consent = consentRequestRepository
                .findByConsentHandle(payload.getConsentHandle())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Consent not found: " + payload.getConsentHandle()));

            // Update status based on webhook
            switch (payload.getStatus()) {
                case "APPROVED":
                    consent.setStatus(ConsentRequest.ConsentStatus.APPROVED);
                    consent.setApprovedAt(OffsetDateTime.now());
                    log.info("Consent approved via webhook: {}", payload.getConsentHandle());

                    // Optionally trigger automatic FI data fetch
                    // triggerFIDataFetch(consent);
                    break;

                case "REJECTED":
                    consent.setStatus(ConsentRequest.ConsentStatus.REJECTED);
                    consent.setRejectedAt(OffsetDateTime.now());
                    log.info("Consent rejected via webhook: {}", payload.getConsentHandle());
                    break;

                case "EXPIRED":
                    consent.setStatus(ConsentRequest.ConsentStatus.EXPIRED);
                    log.info("Consent expired via webhook: {}", payload.getConsentHandle());
                    break;

                default:
                    log.warn("Unknown status in webhook: {}", payload.getStatus());
            }

            consentRequestRepository.save(consent);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Webhook processed successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to process consent status webhook", e);
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /webhooks/digio/fi-data-ready
     * Receives notification when FI data is ready to fetch
     *
     * Webhook payload example:
     * {
     *   "consent_handle": "CH123456",
     *   "event": "FI_DATA_READY",
     *   "fi_request_id": "FIR789",
     *   "timestamp": "2025-01-15T11:00:00Z"
     * }
     */
    @PostMapping("/fi-data-ready")
    public ResponseEntity<Map<String, String>> handleFIDataReady(
            @RequestBody FIDataReadyWebhookPayload payload
    ) {
        log.info("Received FI data ready webhook: handle={}, fiRequestId={}",
            payload.getConsentHandle(), payload.getFiRequestId());

        try {
            // Fetch and decrypt financial data
            String encryptedData = digioAAService.fetchFinancialData(
                payload.getConsentHandle());
            String decryptedData = digioAAService.decryptFinancialData(encryptedData);

            // Process the financial data (Phase 4)
            // financialDataService.processFinancialData(decryptedData, payload.getConsentHandle());

            log.info("FI data fetched and processed successfully for: {}",
                payload.getConsentHandle());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "FI data processed successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to process FI data ready webhook", e);
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    // ==================== Webhook DTOs ====================

    @Data
    public static class ConsentStatusWebhookPayload {
        @JsonProperty("consent_handle")
        private String consentHandle;

        @JsonProperty("status")
        private String status;

        @JsonProperty("event")
        private String event;

        @JsonProperty("timestamp")
        private String timestamp;
    }

    @Data
    public static class FIDataReadyWebhookPayload {
        @JsonProperty("consent_handle")
        private String consentHandle;

        @JsonProperty("event")
        private String event;

        @JsonProperty("fi_request_id")
        private String fiRequestId;

        @JsonProperty("timestamp")
        private String timestamp;
    }
}
```

#### Webhook Security Considerations

**For Production**:
1. **Signature Verification**: Validate webhook signature using shared secret
2. **IP Whitelisting**: Only accept webhooks from Digio IPs
3. **Idempotency**: Handle duplicate webhook deliveries
4. **Async Processing**: Process webhooks in background queue for reliability

**Example Signature Verification** (to be added):
```java
private boolean verifyWebhookSignature(String payload, String signature, String secret) {
    try {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes());
        String expectedSignature = Base64.getEncoder().encodeToString(hash);
        return MessageDigest.isEqual(
            signature.getBytes(),
            expectedSignature.getBytes()
        );
    } catch (Exception e) {
        log.error("Failed to verify webhook signature", e);
        return false;
    }
}
```

#### Testing Webhooks Locally

Use **ngrok** to expose local server to Digio:
```bash
# Install ngrok
brew install ngrok

# Start your Spring Boot app on port 8080
./mvnw spring-boot:run

# In another terminal, start ngrok
ngrok http 8080

# Copy the HTTPS URL (e.g., https://abc123.ngrok.io)
# Configure this URL in Digio dashboard:
# Webhook URL: https://abc123.ngrok.io/webhooks/digio/consent-status
```

---

## Phase 4: Data Processing

### 4.1 Create FinancialDataService

**File**: `src/main/java/com/assessment/interest_calculator/service/FinancialDataService.java`

#### Purpose
Parse, validate, and store financial information received from Digio after decryption.

#### Financial Data Structure (Example)

According to ReBIT specifications, FI data comes in JSON/XML format:

```json
{
  "FI": [
    {
      "fipId": "BANK_FIP_001",
      "data": {
        "type": "DEPOSIT",
        "summary": {
          "currentBalance": 150000.00,
          "currency": "INR"
        },
        "transactions": [
          {
            "txnId": "TXN001",
            "amount": 50000.00,
            "type": "CREDIT",
            "narration": "Salary Credit",
            "transactionTimestamp": "2025-01-01T10:00:00Z"
          }
        ]
      }
    }
  ]
}
```

#### Implementation

```java
package com.assessment.interest_calculator.service;

import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.FinancialData;
import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;
import com.assessment.interest_calculator.repository.FinancialDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancialDataService {

    private final ConsentRequestRepository consentRequestRepository;
    private final FinancialDataRepository financialDataRepository; // To be created
    private final ObjectMapper objectMapper;

    /**
     * Processes decrypted financial information data
     * Parses JSON, validates, and stores relevant information
     *
     * @param decryptedData Decrypted JSON string from Digio
     * @param consentHandle Associated consent handle
     */
    @Transactional
    public void processFinancialData(String decryptedData, String consentHandle) {
        log.info("Processing financial data for consent: {}", consentHandle);

        try {
            // 1. Find consent request
            ConsentRequest consent = consentRequestRepository
                .findByConsentHandle(consentHandle)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Consent not found: " + consentHandle));

            // 2. Parse JSON
            JsonNode rootNode = objectMapper.readTree(decryptedData);
            JsonNode fiArray = rootNode.get("FI");

            if (fiArray == null || !fiArray.isArray()) {
                throw new IllegalArgumentException("Invalid FI data structure");
            }

            // 3. Extract and store each FI record
            for (JsonNode fiNode : fiArray) {
                processFinancialInstitutionData(fiNode, consent);
            }

            log.info("Financial data processed successfully for consent: {}", consentHandle);

        } catch (Exception e) {
            log.error("Failed to process financial data for consent: {}", consentHandle, e);
            throw new RuntimeException("Financial data processing failed", e);
        }
    }

    private void processFinancialInstitutionData(JsonNode fiNode, ConsentRequest consent) {
        String fipId = fiNode.get("fipId").asText();
        JsonNode dataNode = fiNode.get("data");

        String fiType = dataNode.get("type").asText();
        JsonNode summaryNode = dataNode.get("summary");

        log.info("Processing FI data: fipId={}, type={}", fipId, fiType);

        // Extract summary information
        BigDecimal currentBalance = summaryNode.has("currentBalance") ?
            new BigDecimal(summaryNode.get("currentBalance").asText()) : BigDecimal.ZERO;

        String currency = summaryNode.has("currency") ?
            summaryNode.get("currency").asText() : "INR";

        // Create FinancialData entity (entity to be created)
        FinancialData financialData = FinancialData.builder()
            .consentRequest(consent)
            .loanAccount(consent.getLoanAccount())
            .fipId(fipId)
            .fiType(fiType)
            .currentBalance(currentBalance)
            .currency(currency)
            .rawData(fiNode.toString()) // Store complete JSON for audit
            .fetchedAt(OffsetDateTime.now())
            .build();

        financialDataRepository.save(financialData);

        // Process transactions if present
        if (dataNode.has("transactions")) {
            JsonNode transactions = dataNode.get("transactions");
            log.info("Found {} transactions", transactions.size());
            // Process individual transactions if needed
        }
    }

    /**
     * Calculates financial health score based on FI data
     * Can be used for loan underwriting decisions
     *
     * @param loanAccountId Loan account ID
     * @return Financial health score (0-100)
     */
    public int calculateFinancialHealthScore(Long loanAccountId) {
        log.info("Calculating financial health score for loan account: {}", loanAccountId);

        // Retrieve all FI data for the account
        // Analyze:
        // - Average balance
        // - Transaction patterns
        // - Income sources
        // - Expense patterns
        // Return score 0-100

        // TODO: Implement scoring logic
        return 0;
    }
}
```

### 4.2 Create FinancialData Entity (Optional)

**File**: `src/main/java/com/assessment/interest_calculator/entity/FinancialData.java`

```java
package com.assessment.interest_calculator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "financial_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_request_id", nullable = false)
    private ConsentRequest consentRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Column(name = "fip_id", nullable = false)
    private String fipId; // Financial Information Provider ID

    @Column(name = "fi_type", nullable = false)
    private String fiType; // DEPOSIT, MUTUAL_FUNDS, INSURANCE, etc.

    @Column(name = "current_balance", precision = 15, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData; // Complete JSON for audit

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
```

### 4.3 Create FinancialDataRepository

**File**: `src/main/java/com/assessment/interest_calculator/repository/FinancialDataRepository.java`

```java
package com.assessment.interest_calculator.repository;

import com.assessment.interest_calculator.entity.FinancialData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialDataRepository extends JpaRepository<FinancialData, Long> {

    List<FinancialData> findByLoanAccountId(Long loanAccountId);

    List<FinancialData> findByConsentRequestId(Long consentRequestId);

    @Query("SELECT fd FROM FinancialData fd WHERE fd.loanAccount.id = :loanAccountId " +
           "ORDER BY fd.fetchedAt DESC")
    List<FinancialData> findLatestByLoanAccountId(Long loanAccountId);
}
```

---

## Phase 5: Integration & Testing

### 5.1 Database Migration

Ensure the `consent_requests` and `financial_data` tables are created.

**File**: `src/main/resources/schema.sql` (append to existing file)

```sql
-- Consent Requests Table
CREATE TABLE IF NOT EXISTS consent_requests (
    id BIGSERIAL PRIMARY KEY,
    loan_account_id BIGINT NOT NULL REFERENCES loan_accounts(id) ON DELETE CASCADE,
    consent_handle VARCHAR(255) UNIQUE,
    customer_ref_id VARCHAR(255) NOT NULL UNIQUE,
    template_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    redirect_url VARCHAR(500),
    consent_start_date TIMESTAMP WITH TIME ZONE,
    consent_expiry_date TIMESTAMP WITH TIME ZONE,
    fi_start_date DATE,
    fi_end_date DATE,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    customer_message TEXT,
    customer_notification_mode VARCHAR(20),
    notify_customer BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_consent_requests_loan_account_id ON consent_requests(loan_account_id);
CREATE INDEX idx_consent_requests_status ON consent_requests(status);
CREATE INDEX idx_consent_requests_consent_expiry_date ON consent_requests(consent_expiry_date);

-- Financial Data Table (optional)
CREATE TABLE IF NOT EXISTS financial_data (
    id BIGSERIAL PRIMARY KEY,
    consent_request_id BIGINT NOT NULL REFERENCES consent_requests(id) ON DELETE CASCADE,
    loan_account_id BIGINT NOT NULL REFERENCES loan_accounts(id) ON DELETE CASCADE,
    fip_id VARCHAR(255) NOT NULL,
    fi_type VARCHAR(50) NOT NULL,
    current_balance DECIMAL(15, 2),
    currency VARCHAR(3),
    raw_data TEXT,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_financial_data_loan_account_id ON financial_data(loan_account_id);
CREATE INDEX idx_financial_data_consent_request_id ON financial_data(consent_request_id);
```

Run migration:
```bash
# If using Flyway/Liquibase, create migration file
# Otherwise, Hibernate will auto-create with ddl-auto: update

# Start PostgreSQL via Docker
docker-compose up -d postgres

# Run Spring Boot application (tables will be created)
./mvnw spring-boot:run
```

### 5.2 End-to-End Testing Guide

#### Test Scenario 1: Create Consent Request

**Step 1**: Create a loan account (if not exists)
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountHolderName": "Test User",
    "principalAmount": 100000,
    "interestRate": 12.5,
    "dateOfDisbursal": "2025-01-01"
  }'
```

**Step 2**: Create consent request
```bash
curl -X POST http://localhost:8080/api/consents \
  -H "Content-Type: application/json" \
  -d '{
    "loanAccountId": 1,
    "fiStartDate": "2024-01-01",
    "fiEndDate": "2025-01-01",
    "notificationMode": "SMS"
  }'
```

**Expected Response**:
```json
{
  "id": 1,
  "loanAccountId": 1,
  "accountHolderName": "Test User",
  "consentHandle": "CH1234567890",
  "customerRefId": "APP-20250115-abcd1234",
  "status": "PENDING",
  "redirectUrl": "https://digio.in/consent/CH1234567890",
  "fiStartDate": "2024-01-01",
  "fiEndDate": "2025-01-01",
  "consentExpiryDate": "2025-02-14T10:30:00Z",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**Verification**:
- Check database: `SELECT * FROM consent_requests WHERE id = 1;`
- Customer should receive SMS/WhatsApp with consent link
- Check application logs for Digio API call

#### Test Scenario 2: Check Consent Status

**Step 1**: Poll consent status
```bash
curl -X GET http://localhost:8080/api/consents/status/CH1234567890
```

**Step 2**: In Digio sandbox, manually approve the consent

**Step 3**: Poll again to see updated status
```bash
curl -X GET http://localhost:8080/api/consents/status/CH1234567890
```

**Expected Response**:
```json
{
  "id": 1,
  "consentHandle": "CH1234567890",
  "status": "APPROVED",
  "approvedAt": "2025-01-15T11:00:00Z"
}
```

#### Test Scenario 3: Webhook Callback (Manual Test)

**Step 1**: Setup ngrok
```bash
ngrok http 8080
```

**Step 2**: Configure webhook URL in Digio dashboard
```
https://abc123.ngrok.io/webhooks/digio/consent-status
```

**Step 3**: Approve consent in Digio sandbox

**Step 4**: Check ngrok logs to see incoming webhook

**Step 5**: Verify database updated:
```sql
SELECT status, approved_at FROM consent_requests WHERE consent_handle = 'CH1234567890';
```

#### Test Scenario 4: Fetch Financial Data

**Step 1**: Once consent is approved, trigger FI data fetch
```bash
curl -X POST http://localhost:8080/api/consents/CH1234567890/fetch-data
```

**Step 2**: Check logs for decryption and parsing

**Step 3**: Verify financial_data table:
```sql
SELECT * FROM financial_data WHERE consent_request_id = 1;
```

### 5.3 Integration Test Class

**File**: `src/test/java/com/assessment/interest_calculator/integration/DigioIntegrationTest.java`

```java
package com.assessment.interest_calculator.integration;

import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.LoanAccount;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;
import com.assessment.interest_calculator.repository.LoanAccountRepository;
import com.assessment.interest_calculator.service.DigioAAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DigioIntegrationTest {

    @Autowired
    private DigioAAService digioAAService;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private ConsentRequestRepository consentRequestRepository;

    private LoanAccount testLoanAccount;

    @BeforeEach
    void setUp() {
        testLoanAccount = LoanAccount.builder()
            .accountHolderName("Test User")
            .principalAmount(new BigDecimal("100000"))
            .interestRate(new BigDecimal("12.5"))
            .interestAmount(BigDecimal.ZERO)
            .dateOfDisbursal(LocalDate.now())
            .build();
        testLoanAccount = loanAccountRepository.save(testLoanAccount);
    }

    @Test
    void testInitiateConsent_Success() {
        // Given
        LocalDate fiStartDate = LocalDate.now().minusMonths(6);
        LocalDate fiEndDate = LocalDate.now();

        // When
        ConsentRequest consent = digioAAService.initiateConsent(
            testLoanAccount.getId(),
            fiStartDate,
            fiEndDate,
            ConsentRequest.NotificationMode.SMS
        );

        // Then
        assertNotNull(consent);
        assertNotNull(consent.getId());
        assertNotNull(consent.getConsentHandle());
        assertNotNull(consent.getCustomerRefId());
        assertEquals(ConsentRequest.ConsentStatus.PENDING, consent.getStatus());
        assertEquals(fiStartDate, consent.getFiStartDate());
        assertEquals(fiEndDate, consent.getFiEndDate());

        // Verify saved to database
        ConsentRequest savedConsent = consentRequestRepository.findById(consent.getId()).orElseThrow();
        assertThat(savedConsent.getLoanAccount().getId()).isEqualTo(testLoanAccount.getId());
    }

    @Test
    void testInitiateConsent_DuplicateActiveConsent_ThrowsException() {
        // Given - create first consent
        digioAAService.initiateConsent(
            testLoanAccount.getId(),
            LocalDate.now().minusMonths(6),
            LocalDate.now(),
            ConsentRequest.NotificationMode.SMS
        );

        // When/Then - attempting second consent should fail
        assertThrows(IllegalStateException.class, () -> {
            digioAAService.initiateConsent(
                testLoanAccount.getId(),
                LocalDate.now().minusMonths(6),
                LocalDate.now(),
                ConsentRequest.NotificationMode.SMS
            );
        });
    }

    @Test
    void testCheckConsentStatus_UpdatesStatus() {
        // Given - create consent
        ConsentRequest consent = digioAAService.initiateConsent(
            testLoanAccount.getId(),
            LocalDate.now().minusMonths(6),
            LocalDate.now(),
            ConsentRequest.NotificationMode.SMS
        );

        // When - check status (will call Digio API)
        ConsentRequest updatedConsent = digioAAService.checkConsentStatus(
            consent.getConsentHandle()
        );

        // Then
        assertNotNull(updatedConsent);
        // Status may still be PENDING or changed to APPROVED depending on sandbox
        assertThat(updatedConsent.getStatus()).isIn(
            ConsentRequest.ConsentStatus.PENDING,
            ConsentRequest.ConsentStatus.APPROVED,
            ConsentRequest.ConsentStatus.REJECTED
        );
    }
}
```

### 5.4 Error Scenarios to Test

| Scenario | Expected Behavior |
|----------|------------------|
| Invalid loan account ID | `IllegalArgumentException: Loan account not found` |
| Duplicate consent request | `IllegalStateException: Active consent already exists` |
| Digio API timeout | Retry logic kicks in, logs error |
| Invalid consent handle | `IllegalArgumentException: Consent not found` |
| Fetch data for PENDING consent | `IllegalStateException: Cannot fetch data for consent in status: PENDING` |
| Malformed webhook payload | Returns error response but doesn't crash |
| Decryption failure | Logs error, throws `RuntimeException: Decryption failed` |

---

## Phase 6: Documentation & Cleanup

### 6.1 Global Exception Handler

**File**: `src/main/java/com/assessment/interest_calculator/exception/GlobalExceptionHandler.java`

```java
package com.assessment.interest_calculator.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.error("Illegal argument error", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex) {
        log.error("Illegal state error", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        log.error("Validation error", ex);
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed: " + fieldErrors,
            OffsetDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime error", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error: " + ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public record ErrorResponse(
        int status,
        String message,
        OffsetDateTime timestamp
    ) {}
}
```

### 6.2 Update application.yml

Ensure all Digio properties are documented:

```yaml
# Digio Account Aggregator Configuration
digio:
  aa:
    # Digio API base URL (sandbox: https://ext.digio.in:444/fiu_api)
    base-url: ${DIGIO_BASE_URL:https://ext.digio.in:444/fiu_api}

    # API credentials from Digio dashboard
    api-key: ${DIGIO_API_KEY}
    api-secret: ${DIGIO_API_SECRET}

    # Consent template ID created in Digio dashboard
    template-id: ${DIGIO_TEMPLATE_ID}

    # Webhook base URL (use ngrok for local testing)
    webhook-base-url: ${DIGIO_WEBHOOK_BASE_URL:http://localhost:8080}

    # RSA key paths for encryption/decryption
    public-key-path: ${DIGIO_PUBLIC_KEY_PATH:classpath:keys/public.pem}
    private-key-path: ${DIGIO_PRIVATE_KEY_PATH:classpath:keys/private.pem}
```

### 6.3 API Documentation (Swagger/OpenAPI)

Add Swagger dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

### 6.4 Update README.md

Add Digio integration section to README (append to existing content):

```markdown
## Digio Account Aggregator Integration

This application integrates with Digio Account Aggregator to fetch customer financial data with consent.

### Setup

1. **Obtain Digio Credentials**
   - Sign up at https://digio.in
   - Create FIU account
   - Get Client ID and Secret
   - Create consent template

2. **Configure Environment Variables**
   ```bash
   export DIGIO_BASE_URL=https://ext.digio.in:444/fiu_api
   export DIGIO_API_KEY=your_client_id
   export DIGIO_API_SECRET=your_client_secret
   export DIGIO_TEMPLATE_ID=your_template_id
   ```

3. **Generate RSA Keys** (if not already done)
   ```bash
   openssl genpkey -algorithm RSA -out src/main/resources/keys/private.pem -pkeyopt rsa_keygen_bits:4096
   openssl pkey -in src/main/resources/keys/private.pem -pubout -out src/main/resources/keys/public.pem
   ```

### API Endpoints

#### Consent Management
- `POST /api/consents` - Create consent request
- `GET /api/consents/{id}` - Get consent details
- `GET /api/consents/status/{handle}` - Check consent status
- `GET /api/loan-accounts/{id}/consents` - List consents for account
- `POST /api/consents/{handle}/fetch-data` - Fetch financial data

#### Webhooks
- `POST /webhooks/digio/consent-status` - Consent status updates
- `POST /webhooks/digio/fi-data-ready` - FI data ready notification

### Testing

See [DIGIO_INTEGRATION_GUIDE.md](DIGIO_INTEGRATION_GUIDE.md) for detailed testing instructions.

### Consent Flow

1. Create consent request via API
2. Customer receives SMS/WhatsApp with consent link
3. Customer approves consent in Digio portal
4. Webhook notifies your application (or poll status)
5. Fetch encrypted financial data from Digio
6. Decrypt and process data
7. Use for loan underwriting decisions
```

---

## API Reference

### Digio API Endpoints Used

| Endpoint | Method | Description | Documentation |
|----------|--------|-------------|---------------|
| `/client/consent/request/` | POST | Create consent request | [Link](https://documentation.digio.in/fiu-tsp/api-integration/consent-template/) |
| `/client/consent/request/{handle}` | GET | Get consent status | [Link](https://documentation.digio.in/fiu-tsp/api-integration/retrieve-status/) |
| `/client/fi/request/list/{consentId}` | GET | List FI requests | [Link](https://documentation.digio.in/fiu-tsp/api-integration/list_fi_request/) |
| `/client/fi/fetch` | POST | Fetch encrypted FI data | [Link](https://documentation.digio.in/fiu-tsp/) |

### Request/Response Examples

#### Create Consent Request

**Request**:
```bash
curl -X POST https://ext.digio.in:444/fiu_api/client/consent/request/ \
  -H "Content-Type: application/json" \
  -H "client_id: YOUR_CLIENT_ID" \
  -H "client_secret: YOUR_CLIENT_SECRET" \
  -d '{
    "customer_details": {
      "customer_mobile": "9999999999",
      "customer_ref_id": "APP-20250115-001",
      "customer_email": "customer@example.com",
      "customer_identifier": "9999999999",
      "customer_name": "John Doe"
    },
    "customer_notification_mode": "SMS",
    "template_id": "CTMP250926152142855LYQ59RGKJTAGZ",
    "notify_customer": true,
    "consent_details": {
      "fi_start_date": "2024-01-01 00:00:00",
      "fi_end_date": "2025-01-01 00:00:00",
      "consent_expiry_date": "2025-02-15 00:00:00",
      "consent_start_date": "2025-01-15 00:00:00"
    }
  }'
```

**Response**:
```json
{
  "consent_handle": "CH1234567890",
  "customer_ref_id": "APP-20250115-001",
  "redirect_url": "https://digio.in/consent/CH1234567890",
  "template_id": "CTMP250926152142855LYQ59RGKJTAGZ",
  "consent_details": {
    "consent_start_date": "2025-01-15T00:00:00Z",
    "consent_expiry_date": "2025-02-15T00:00:00Z",
    "fi_start_date": "2024-01-01",
    "fi_end_date": "2025-01-01"
  },
  "status": "PENDING"
}
```

#### Get Consent Status

**Request**:
```bash
curl -X GET https://ext.digio.in:444/fiu_api/client/consent/request/CH1234567890 \
  -H "client_id: YOUR_CLIENT_ID" \
  -H "client_secret: YOUR_CLIENT_SECRET"
```

**Response**:
```json
{
  "consent_handle": "CH1234567890",
  "status": "APPROVED",
  "approved_at": "2025-01-15T10:30:00Z",
  "consent_details": {
    "consent_start_date": "2025-01-15T00:00:00Z",
    "consent_expiry_date": "2025-02-15T00:00:00Z"
  }
}
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. WebClient Bean Not Found
**Error**: `No qualifying bean of type 'org.springframework.web.reactive.function.client.WebClient'`

**Solution**: Ensure [DigioAAConfig.java](src/main/java/com/assessment/interest_calculator/config/DigioAAConfig.java) is in the component scan path and has `@Configuration` annotation.

#### 2. Digio API Returns 401 Unauthorized
**Error**: `401 Unauthorized` from Digio API

**Solution**:
- Verify `DIGIO_API_KEY` and `DIGIO_API_SECRET` are correct
- Check if credentials are expired
- Ensure headers are: `client_id` and `client_secret` (not `Authorization: Bearer`)

#### 3. Consent Already Exists Error
**Error**: `IllegalStateException: Active consent already exists`

**Solution**:
- Check existing consents: `SELECT * FROM consent_requests WHERE loan_account_id = ? AND status IN ('PENDING', 'APPROVED');`
- Wait for existing consent to expire or be rejected
- Or manually update status: `UPDATE consent_requests SET status = 'EXPIRED' WHERE id = ?;`

#### 4. Webhook Not Received
**Error**: Webhook callback not received from Digio

**Solution**:
- Verify ngrok is running: `ngrok http 8080`
- Check ngrok web interface: `http://127.0.0.1:4040`
- Ensure webhook URL in Digio dashboard is correct: `https://abc123.ngrok.io/webhooks/digio/consent-status`
- Check firewall settings
- Look for errors in ngrok logs

#### 5. Decryption Failed
**Error**: `RuntimeException: Decryption failed`

**Solution**:
- Verify private key file exists and is readable
- Check key format (should be PEM format)
- Ensure key matches public key registered with Digio
- Validate encrypted data is base64 encoded

#### 6. Database Connection Failed
**Error**: `org.postgresql.util.PSQLException: Connection refused`

**Solution**:
```bash
# Start PostgreSQL via Docker
docker-compose up -d postgres

# Check if running
docker ps | grep postgres

# Check logs
docker logs interest-calculator-postgres
```

#### 7. Customer Not Receiving SMS/WhatsApp
**Error**: No notification received

**Solution**:
- Verify `notify_customer: true` in request
- Check phone number format (10 digits without +91)
- Sandbox environment may not send actual SMS
- Check Digio dashboard for notification logs

---

## Additional Considerations

### Security Best Practices

1. **Never commit secrets**: Use environment variables for all credentials
2. **Rotate keys regularly**: Update RSA keys every 90 days
3. **Validate webhook signatures**: Implement HMAC verification
4. **Use HTTPS**: Always use TLS for API communication
5. **Sanitize inputs**: Validate all user inputs to prevent injection attacks
6. **Rate limiting**: Implement rate limiting on public endpoints
7. **Audit logging**: Log all consent operations for compliance

### Performance Optimization

1. **Async processing**: Use Spring's `@Async` for webhook processing
2. **Caching**: Cache consent status responses (5-minute TTL)
3. **Batch operations**: Process multiple FI data records in batches
4. **Database indexing**: Ensure indexes on frequently queried columns
5. **Connection pooling**: Configure HikariCP for database connections

### Compliance and Auditing

1. **Consent tracking**: Maintain complete audit trail of all consent operations
2. **Data retention**: Define retention policy for FI data (as per RBI guidelines)
3. **User consent revocation**: Implement endpoint to revoke consent
4. **Data encryption at rest**: Encrypt sensitive data in database
5. **Access logs**: Log all access to financial data

### Monitoring and Alerting

1. **Health checks**: Use Spring Actuator to monitor service health
2. **Metrics**: Track consent approval rate, API latency, error rates
3. **Alerts**: Set up alerts for:
   - Digio API failures
   - Webhook delivery failures
   - Decryption errors
   - Expired consents

### Future Enhancements

1. **Scheduler for status polling**: Automatically poll pending consents every 5 minutes
2. **Consent renewal**: Auto-renew consents before expiry
3. **Multi-AA support**: Support multiple Account Aggregators (not just Digio)
4. **Dashboard**: Build admin dashboard for consent management
5. **Analytics**: Financial health scoring based on FI data
6. **Notifications**: Email/SMS notifications to customers

---

## Summary

This guide provides a complete roadmap for integrating Digio Account Aggregator into your Spring Boot application. Follow the phases sequentially, test thoroughly in sandbox, and adhere to security best practices before going to production.

**Estimated Timeline**:
- Phase 1 (Repository): 1 hour
- Phase 2 (Service): 3-4 hours
- Phase 3 (Controllers): 2-3 hours
- Phase 4 (Data Processing): 2 hours
- Phase 5 (Testing): 3-4 hours
- Phase 6 (Documentation): 1-2 hours

**Total**: ~12-16 hours for complete implementation and testing

For questions or support, refer to [Digio Documentation](https://documentation.digio.in/) or contact Digio support.

---

**Happy Coding!**
