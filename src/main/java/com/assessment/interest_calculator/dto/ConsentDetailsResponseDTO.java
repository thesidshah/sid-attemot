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
public class ConsentDetailsResponseDTO {
    
    @JsonProperty("consent_request_id")
    private String consentRequestId;
    
    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("consent_details")
    private ConsentDetails consentDetails;
    
    @JsonProperty("request_expire_date")
    private String requestExpireDate;
    
    @JsonProperty("fi_schedule")
    private ConsentResponseDTO.FiSchedule fiSchedule;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetails {
        @JsonProperty("customer_name")
        private String customerName;
        
        @JsonProperty("customer_email")
        private String customerEmail;
        
        @JsonProperty("customer_ref_id")
        private String customerRefId;
        
        @JsonProperty("customer_mobile")
        private String customerMobile;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentDetails {
        @JsonProperty("consent_start_date")
        private String consentStartDate;
        
        @JsonProperty("consent_expiry_date")
        private String consentExpiryDate;
        
        @JsonProperty("fi_start_date")
        private String fiStartDate;
        
        @JsonProperty("fi_end_date")
        private String fiEndDate;
    }
}
