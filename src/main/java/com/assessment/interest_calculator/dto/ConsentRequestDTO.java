package com.assessment.interest_calculator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRequestDTO {
    @JsonProperty("customer_details")
    @NotNull(message = "Customer details are required")
    @Valid
    private CustomerDetails customerDetails;

    @JsonProperty("customer_notification_mode")
    @NotBlank(message = "Customer notification mode is required")
    private String customerNotificationMode;

    @JsonProperty("template_id")
    @NotBlank(message = "Template ID is required")
    private String templateId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("notify_customer")
    @Builder.Default
    private boolean notifyCustomer = true;

    @JsonProperty("consent_details")
    @NotNull(message = "Consent details are required")
    @Valid
    private ConsentDetails consentDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerDetails {
        @JsonProperty("customer_name")
        @NotBlank(message = "Customer name is required")
        private String customerName;

        @JsonProperty("customer_email")
        private String customerEmail;

        @JsonProperty("customer_ref_id")
        @NotBlank(message = "Customer reference ID is required")
        private String customerRefId;

        @JsonProperty("customer_mobile")
        private String customerMobile;

        /**
         * This identifier should match with the mode of communication preferred by the customer.
         */
        @JsonProperty("customer_identifier")
        @NotBlank(message = "Customer identifier is required")
        private String customerIdentifier;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConsentDetails {
        @JsonProperty("fi_start_date")
        @NotBlank(message = "FI start date is required")
        private String fiStartDate;

        @JsonProperty("fi_end_date")
        @NotBlank(message = "FI end date is required")
        private String fiEndDate;

        @JsonProperty("consent_expiry_date")
        @NotBlank(message = "Consent expiry date is required")
        private String consentExpiryDate;

        @JsonProperty("consent_start_date")
        @NotBlank(message = "Consent start date is required")
        private String consentStartDate;
    }
}