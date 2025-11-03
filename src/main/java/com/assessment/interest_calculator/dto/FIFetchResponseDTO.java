package com.assessment.interest_calculator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FIFetchResponseDTO {
    
    @JsonProperty("fi_request_id")
    private String fiRequestId;
    
    @JsonProperty("fi_start_date")
    private String fiStartDate;
    
    @JsonProperty("fi_end_date")
    private String fiEndDate;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("fi")
    private List<FinancialInformation> fi;
    
    @JsonProperty("consent_request_id")
    private String consentRequestId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialInformation {
        
        @JsonProperty("fi_data")
        private Map<String, Object> fiData;
        
        @JsonProperty("fip_id")
        private String fipId;
        
        @JsonProperty("fi_type")
        private String fiType;
        
        @JsonProperty("account_number")
        private String accountNumber;
        
        @JsonProperty("account_analytics_available")
        private Boolean accountAnalyticsAvailable;
        
        @JsonProperty("account_sub_analytics_available")
        private Boolean accountSubAnalyticsAvailable;
        
        @JsonProperty("account_id")
        private String accountId;
        
        @JsonProperty("fi_data_id")
        private String fiDataId;
    }
}
