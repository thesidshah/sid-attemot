package com.assessment.interest_calculator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.assessment.interest_calculator.dto.ConsentRequestDTO;
import com.assessment.interest_calculator.dto.ConsentResponseDTO;
import com.assessment.interest_calculator.dto.ConsentStatusResponse;
import com.assessment.interest_calculator.entity.ConsentRequest.NotificationMode;
import com.assessment.interest_calculator.service.ConsentService;

import reactor.core.publisher.Mono;

/**
 * Test class for ConsentController.
 * Tests consent request creation and status check endpoints.
 */
@WebMvcTest(ConsentController.class)
class ConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentService consentService;

    private ConsentResponseDTO mockConsentResponse;
    private ConsentStatusResponse mockStatusResponse;
    private OffsetDateTime now;
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now(IST_ZONE);

        // Setup mock consent response
        mockConsentResponse = ConsentResponseDTO.builder()
                .consentRequestId("CRID123456789")
                .status("PENDING")
                .customerDetails(ConsentResponseDTO.CustomerDetails.builder()
                        .id("CUST001")
                        .customerName("Siddhant Shah")
                        .customerEmail("shah.sid@northeastern.edu")
                        .customerMobile("8574379424")
                        .customerRefId("APP-20251106-TEST-001")
                        .customerIdentifier("8574379424")
                        .build())
                .consentDetails(ConsentResponseDTO.ConsentDetails.builder()
                        .consentStartDate("2025-11-06 12:00:00")
                        .consentExpiryDate("2025-12-06 23:59:59")
                        .fiStartDate("2024-09-17 00:04:14")
                        .fiEndDate("2025-10-17 00:04:14")
                        .build())
                .templateId("CTMP250926152142855LYQ59RGKJTAGZ")
                .build();

        // Setup mock status response
        mockStatusResponse = ConsentStatusResponse.builder()
                .consentHandle("CRID123456789")
                .status("PENDING")
                .redirectUrl("https://example.com/consent")
                .createdAt(now)
                .expiresAt(now.plusDays(30))
                .message("Consent request is pending customer approval")
                .build();
    }

    @Test
    void testInitiateConsentRequest_WithValidData_ShouldReturnCreated() throws Exception {
        // Arrange
        when(consentService.initiateConsentRequest(any(ConsentRequestDTO.class), eq(NotificationMode.SMS)))
                .thenReturn(Mono.just(mockConsentResponse));

        String requestBody = """
                {
                    "customer_details": {
                        "customer_mobile": "8574379424",
                        "customer_ref_id": "APP-20251106-TEST-001",
                        "customer_email": "shah.sid@northeastern.edu",
                        "customer_identifier": "8574379424",
                        "customer_name": "Siddhant Shah"
                    },
                    "customer_notification_mode": "SMS",
                    "template_id": "CTMP250926152142855LYQ59RGKJTAGZ",
                    "customer_id": "",
                    "notify_customer": true,
                    "consent_details": {
                        "fi_start_date": "2024-09-17 00:04:14",
                        "fi_end_date": "2025-10-17 00:04:14",
                        "consent_expiry_date": "2025-12-06 23:59:59",
                        "consent_start_date": "2025-11-06 12:00:00"
                    }
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/consent/request")
                        .param("notificationMode", "SMS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consent_request_id").value("CRID123456789"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customer_details.customer_name").value("Siddhant Shah"))
                .andExpect(jsonPath("$.customer_details.customer_ref_id").value("APP-20251106-TEST-001"))
                .andExpect(jsonPath("$.customer_details.customer_mobile").value("8574379424"))
                .andExpect(jsonPath("$.template_id").value("CTMP250926152142855LYQ59RGKJTAGZ"));

        verify(consentService, times(1)).initiateConsentRequest(any(ConsentRequestDTO.class), eq(NotificationMode.SMS));
    }

    @Test
    void testInitiateConsentRequest_WithEmailNotification_ShouldReturnCreated() throws Exception {
        // Arrange
        when(consentService.initiateConsentRequest(any(ConsentRequestDTO.class), eq(NotificationMode.EMAIL)))
                .thenReturn(Mono.just(mockConsentResponse));

        String requestBody = """
                {
                    "customer_details": {
                        "customer_mobile": "8574379424",
                        "customer_ref_id": "APP-20251106-TEST-002",
                        "customer_email": "shah.sid@northeastern.edu",
                        "customer_identifier": "shah.sid@northeastern.edu",
                        "customer_name": "Siddhant Shah"
                    },
                    "customer_notification_mode": "EMAIL",
                    "template_id": "CTMP250926152142855LYQ59RGKJTAGZ",
                    "customer_id": "",
                    "notify_customer": true,
                    "consent_details": {
                        "fi_start_date": "2024-09-17 00:04:14",
                        "fi_end_date": "2025-10-17 00:04:14",
                        "consent_expiry_date": "2025-12-06 23:59:59",
                        "consent_start_date": "2025-11-06 12:00:00"
                    }
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/consent/request")
                        .param("notificationMode", "EMAIL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consent_request_id").value("CRID123456789"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(consentService, times(1)).initiateConsentRequest(any(ConsentRequestDTO.class), eq(NotificationMode.EMAIL));
    }

    @Test
    void testInitiateConsentRequest_WithMissingCustomerName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "customer_details": {
                        "customer_mobile": "8574379424",
                        "customer_ref_id": "APP-20251106-TEST-001",
                        "customer_email": "shah.sid@northeastern.edu",
                        "customer_identifier": "8574379424"
                    },
                    "customer_notification_mode": "SMS",
                    "template_id": "CTMP250926152142855LYQ59RGKJTAGZ",
                    "customer_id": "",
                    "notify_customer": true,
                    "consent_details": {
                        "fi_start_date": "2024-09-17 00:04:14",
                        "fi_end_date": "2025-10-17 00:04:14",
                        "consent_expiry_date": "2025-12-06 23:59:59",
                        "consent_start_date": "2025-11-06 12:00:00"
                    }
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/consent/request")
                        .param("notificationMode", "SMS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInitiateConsentRequest_WithMissingConsentDetails_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "customer_details": {
                        "customer_mobile": "8574379424",
                        "customer_ref_id": "APP-20251106-TEST-001",
                        "customer_email": "shah.sid@northeastern.edu",
                        "customer_identifier": "8574379424",
                        "customer_name": "Siddhant Shah"
                    },
                    "customer_notification_mode": "SMS",
                    "template_id": "CTMP250926152142855LYQ59RGKJTAGZ",
                    "customer_id": "",
                    "notify_customer": true
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/consent/request")
                        .param("notificationMode", "SMS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetConsentStatusByCustomerRefId_WithValidRefId_ShouldReturnOk() throws Exception {
        // Arrange
        String customerRefId = "APP-20251106-TEST-001";
        when(consentService.getConsentStatusByCustomerRefId(customerRefId))
                .thenReturn(Mono.just(mockStatusResponse));

        // Act & Assert
        mockMvc.perform(get("/api/consent/status/customer/{customerRefId}", customerRefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentHandle").value("CRID123456789"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Consent request is pending customer approval"))
                .andExpect(jsonPath("$.redirectUrl").value("https://example.com/consent"));

        verify(consentService, times(1)).getConsentStatusByCustomerRefId(customerRefId);
    }

    @Test
    void testGetConsentStatusByCustomerRefId_WithNonExistentRefId_ShouldReturnError() throws Exception {
        // Arrange
        String customerRefId = "NON-EXISTENT-ID";
        when(consentService.getConsentStatusByCustomerRefId(customerRefId))
                .thenReturn(Mono.error(new IllegalArgumentException("Consent request not found for customerRefId: " + customerRefId)));

        // Act & Assert
        mockMvc.perform(get("/api/consent/status/customer/{customerRefId}", customerRefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(consentService, times(1)).getConsentStatusByCustomerRefId(customerRefId);
    }

    @Test
    void testGetConsentStatusByHandle_WithValidHandle_ShouldReturnOk() throws Exception {
        // Arrange
        String consentHandle = "CRID123456789";
        when(consentService.getConsentStatusByHandle(consentHandle))
                .thenReturn(Mono.just(mockStatusResponse));

        // Act & Assert
        mockMvc.perform(get("/api/consent/status/handle/{consentHandle}", consentHandle)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentHandle").value("CRID123456789"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Consent request is pending customer approval"));

        verify(consentService, times(1)).getConsentStatusByHandle(consentHandle);
    }

    @Test
    void testGetConsentStatusByHandle_WithNonExistentHandle_ShouldReturnError() throws Exception {
        // Arrange
        String consentHandle = "NON-EXISTENT-HANDLE";
        when(consentService.getConsentStatusByHandle(consentHandle))
                .thenReturn(Mono.error(new IllegalArgumentException("Consent request not found for consentHandle: " + consentHandle)));

        // Act & Assert
        mockMvc.perform(get("/api/consent/status/handle/{consentHandle}", consentHandle)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(consentService, times(1)).getConsentStatusByHandle(consentHandle);
    }

    @Test
    void testGetConsentStatusByCustomerRefId_WithApprovedStatus_ShouldReturnApprovedStatus() throws Exception {
        // Arrange
        String customerRefId = "APP-20251106-APPROVED";
        ConsentStatusResponse approvedResponse = ConsentStatusResponse.builder()
                .consentHandle("CRID987654321")
                .status("APPROVED")
                .redirectUrl("https://example.com/consent")
                .createdAt(now)
                .expiresAt(now.plusDays(30))
                .message("Consent has been approved by customer")
                .build();

        when(consentService.getConsentStatusByCustomerRefId(customerRefId))
                .thenReturn(Mono.just(approvedResponse));

        // Act & Assert
        mockMvc.perform(get("/api/consent/status/customer/{customerRefId}", customerRefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentHandle").value("CRID987654321"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.message").value("Consent has been approved by customer"));

        verify(consentService, times(1)).getConsentStatusByCustomerRefId(customerRefId);
    }

    @Test
    void testInitiateConsentRequest_WithDefaultNotificationMode_ShouldUseSMS() throws Exception {
        // Arrange
        when(consentService.initiateConsentRequest(any(ConsentRequestDTO.class), eq(NotificationMode.SMS)))
                .thenReturn(Mono.just(mockConsentResponse));

        String requestBody = """
                {
                    "customer_details": {
                        "customer_mobile": "8574379424",
                        "customer_ref_id": "APP-20251106-TEST-003",
                        "customer_email": "shah.sid@northeastern.edu",
                        "customer_identifier": "8574379424",
                        "customer_name": "Siddhant Shah"
                    },
                    "customer_notification_mode": "SMS",
                    "template_id": "CTMP250926152142855LYQ59RGKJTAGZ",
                    "customer_id": "",
                    "notify_customer": true,
                    "consent_details": {
                        "fi_start_date": "2024-09-17 00:04:14",
                        "fi_end_date": "2025-10-17 00:04:14",
                        "consent_expiry_date": "2025-12-06 23:59:59",
                        "consent_start_date": "2025-11-06 12:00:00"
                    }
                }
                """;

        // Act & Assert - without specifying notificationMode parameter, should default to SMS
        mockMvc.perform(post("/api/consent/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consent_request_id").value("CRID123456789"));

        verify(consentService, times(1)).initiateConsentRequest(any(ConsentRequestDTO.class), eq(NotificationMode.SMS));
    }
}
