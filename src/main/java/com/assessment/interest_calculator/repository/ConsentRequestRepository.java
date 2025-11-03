package com.assessment.interest_calculator.repository;

import com.assessment.interest_calculator.entity.ConsentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRequestRepository extends JpaRepository<ConsentRequest, Long> {
    
    Optional<ConsentRequest> findByConsentRequestId(String consentRequestId);
    
    Optional<ConsentRequest> findByCustomerRefId(String customerRefId);
    
    Optional<ConsentRequest> findByFiRequestId(String fiRequestId);
    
    @Query("SELECT cr FROM ConsentRequest cr WHERE cr.loanAccount.id = :loanAccountId " +
           "ORDER BY cr.createdAt DESC")
    List<ConsentRequest> findByLoanAccountId(@Param("loanAccountId") Long loanAccountId);
    
    @Query("SELECT cr FROM ConsentRequest cr WHERE cr.status = :status " +
           "AND cr.consentExpiryDate < :now")
    List<ConsentRequest> findExpiredConsents(
        @Param("status") ConsentRequest.ConsentStatus status,
        @Param("now") OffsetDateTime now
    );
}
