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
public class ConsentResponse {
    @JsonProperty("consent_request_id")
    private String consentRequestId;

    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;

    @JsonProperty("status")
    private String status;

    @JsonProperty("consent_details")
    private ConsentDetails consentDetails;

    @JsonProperty("data_frequency_value")
    private Integer dataFrequencyValue;

    @JsonProperty("data_frequency_unit")
    private String dataFrequencyUnit;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("consent_expire_date")
    private String consentExpireDate;

    @JsonProperty("request_expire_date")
    private String requestExpireDate;

    @JsonProperty("template_name")
    private String templateName;

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("aa_id")
    private String aaId;

    @JsonProperty("require_analytics")
    private boolean requireAnalytics;

    @JsonProperty("gateway_token_id")
    private String gatewayTokenId;

    @JsonProperty("fi_schedule")
    private FiSchedule fiSchedule;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetails {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("customer_name")
        private String customerName;
        
        @JsonProperty("customer_email")
        private String customerEmail;
        
        @JsonProperty("customer_ref_id")
        private String customerRefId;
        
        @JsonProperty("customer_mobile")
        private String customerMobile;
        
        @JsonProperty("customer_identifier")
        private String customerIdentifier;
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
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FiSchedule {
        @JsonProperty("daily_frequency_meta")
        private DailyFrequencyMeta dailyFrequencyMeta;
        
        @JsonProperty("weekly_frequency_meta")
        private WeeklyFrequencyMeta weeklyFrequencyMeta;
        
        @JsonProperty("monthly_frequency_meta")
        private MonthlyFrequencyMeta monthlyFrequencyMeta;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyFrequencyMeta {
        @JsonProperty("fi_request_time")
        private String fiRequestTime;
        
        @JsonProperty("fetch_on_week_days_only")
        private Boolean fetchOnWeekDaysOnly;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyFrequencyMeta {
        @JsonProperty("fi_request_time")
        private String fiRequestTime;
        
        @JsonProperty("days_of_week")
        private List<Integer> daysOfWeek;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyFrequencyMeta {
        @JsonProperty("fi_request_time")
        private String fiRequestTime;
        
        @JsonProperty("dates_of_month")
        private List<Integer> datesOfMonth;
    }
}
