package com.assessment.interest_calculator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fip_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FIPAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_data_id", nullable = false)
    private FinancialData financialData;
    
    @Column(name = "fip_id", nullable = false)
    private String fipId;
    
    @Column(name = "fi_type", nullable = false)
    private String fiType;
    
    @Column(name = "account_number")
    private String accountNumber;
    
    @Column(name = "account_id")
    private String accountId;
    
    @Column(name = "fi_data_id")
    private String fiDataId;
    
    @Column(name = "account_analytics_available")
    private Boolean accountAnalyticsAvailable;
    
    @Column(name = "account_sub_analytics_available")
    private Boolean accountSubAnalyticsAvailable;
    
    @Column(name = "fi_data", columnDefinition = "TEXT")
    private String fiData;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
