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
public class ConsentRequestDTO {
    
    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;
    
    @JsonProperty("template_id")
    private String templateId;
    
    @JsonProperty("consent_details")
    private ConsentDetails consentDetails;
    
    @JsonProperty("customer_notification_mode")
    @Builder.Default
    private String customerNotificationMode = "SMS";
    
    @JsonProperty("notify_customer")
    @Builder.Default
    private Boolean notifyCustomer = true;
    
    @JsonProperty("fi_schedule")
    private FiSchedule fiSchedule;
    
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
        
        @JsonProperty("meta")
        private ConsentMeta meta;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentMeta {
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
        private Boolean showConsentInfo = true;
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
