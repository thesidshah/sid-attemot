package com.assessment.interest_calculator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "financial_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_request_id", nullable = false, unique = true)
    private ConsentRequest consentRequest;

    @Column(name = "fi_request_id", unique = true)
    private String fiRequestId;

    @Column(name = "fi_status")
    private String fiStatus;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "fip_count")
    private Integer fipCount;

    @Column(name = "account_count")
    private Integer accountCount;

    @Column(name = "data_fetched_at")
    private OffsetDateTime dataFetchedAt;

    @Column(name = "data_expires_at", nullable = false)
    private OffsetDateTime dataExpiresAt;

    @Column(name = "is_purged", nullable = false)
    @Builder.Default
    private Boolean isPurged = false;

    @Column(name = "purged_at")
    private OffsetDateTime purgedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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
}
