package com.assessment.interest_calculator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private CustomerDetails customerDetails;

    @JsonProperty("customer_notification_mode")
    private String customerNotificationMode;

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("notify_customer")
    @Builder.Default
    private boolean notifyCustomer = true;

    @JsonProperty("consent_details")
    private ConsentDetails consentDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerDetails {
        @JsonProperty("customer_name")
        private String customerName;

        @JsonProperty("customer_email")
        private String customerEmail;

        @JsonProperty("customer_ref_id")
        private String customerRefId;

        @JsonProperty("customer_mobile")
        private String customerMobile;

        /**
         * This identifier should match with the mode of communication preferred by the customer.
         */
        @JsonProperty("customer_identifier")
        private String customerIdentifier;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConsentDetails {
        @JsonProperty("fi_start_date")
        private String fiStartDate;

        @JsonProperty("fi_end_date")
        private String fiEndDate;

        @JsonProperty("consent_expiry_date")
        private String consentExpiryDate;

        @JsonProperty("consent_start_date")
        private String consentStartDate;
    }
}