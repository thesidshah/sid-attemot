package com.assessment.interest_calculator.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRequest {
    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;

    

@Data
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

    /**
     * This identifier should match with the mode of communication preferred by the customer.
     */
    @JsonProperty("customer_identifier")
    private String customerIdentifier;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class ConsentData {
    @JsonProperty("fip_ids")
    private List<String> fipIds;

    @JsonProperty("email_id")
    private String emailId;

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("dob")
    private String dob;

    @JsonProperty("show_consent_info")
    @Builder.Default
    private boolean showConsentInfo = true;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class FiSchedule {
     //TODO: Not implemented yet
    @JsonProperty("daily_frequency_meta")
    private DailyFrequencyMeta dailyFrequencyMeta;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class DailyFrequencyMeta {
    @JsonProperty("fi_request_time")
    private String fiRequestTime;

    @JsonProperty("fetch_on_week_days_only")
    private boolean fetchOnWeekDaysOnly;
}
}