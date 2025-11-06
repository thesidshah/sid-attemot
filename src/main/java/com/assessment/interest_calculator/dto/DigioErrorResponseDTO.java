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
public class DigioErrorResponseDTO {
    @JsonProperty("details")
    private String details;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("errorMsg")
    private String errorMsg;

    @JsonProperty("ver")
    private String ver;
}
