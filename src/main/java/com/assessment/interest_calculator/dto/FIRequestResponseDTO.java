package com.assessment.interest_calculator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FIRequestResponseDTO {
    
    @JsonProperty("fi_request_id")
    private String fiRequestId;
    
    @JsonProperty("fi_start_date")
    private String fiStartDate;
    
    @JsonProperty("fi_end_date")
    private String fiEndDate;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("consent_request_id")
    private String consentRequestId;
}
