package com.assessment.interest_calculator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FIRequestDetailsResponseDTO {
    
    @JsonProperty("fi_request_id")
    private String fiRequestId;
    
    @JsonProperty("fi_start_date")
    private String fiStartDate;
    
    @JsonProperty("fi_end_date")
    private String fiEndDate;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("accounts")
    private List<AccountInfo> accounts;
    
    @JsonProperty("consent_request_id")
    private String consentRequestId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {
        @JsonProperty("account_id")
        private String accountId;
        
        @JsonProperty("link_ref_number")
        private String linkRefNumber;
        
        @JsonProperty("masked_account_number")
        private String maskedAccountNumber;
        
        @JsonProperty("fi_type")
        private String fiType;
        
        @JsonProperty("fip_id")
        private String fipId;
    }
}
