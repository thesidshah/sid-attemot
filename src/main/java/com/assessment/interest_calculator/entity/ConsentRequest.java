package com.assessment.interest_calculator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "consent_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;
    
    @Column(name = "consent_handle", unique = true)
    private String consentHandle;
    
    @Column(name = "customer_ref_id", nullable = false, unique = true)
    private String customerRefId;
    
    @Column(name = "template_id", nullable = false)
    private String templateId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;
    
    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;
    
    @Column(name = "consent_start_date")
    private OffsetDateTime consentStartDate;
    
    @Column(name = "consent_expiry_date")
    private OffsetDateTime consentExpiryDate;
    
    @Column(name = "fi_start_date")
    private LocalDate fiStartDate;
    
    @Column(name = "fi_end_date")
    private LocalDate fiEndDate;
    
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    
    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;
    
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name="notify_customer", nullable = false)
    @Builder.Default
    private boolean notifyCustomer = true;
    
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
    
    public enum ConsentStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED,
        DATA_FETCHED
    }
}

/**
 * I have not included fiSchedule and related classes here as they do not seem to be part of the ConsentRequest entity based on the provided context.
 */