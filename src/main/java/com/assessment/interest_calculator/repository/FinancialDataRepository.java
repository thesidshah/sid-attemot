package com.assessment.interest_calculator.repository;

import com.assessment.interest_calculator.entity.ConsentRequest;
import com.assessment.interest_calculator.entity.FinancialData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialDataRepository extends JpaRepository<FinancialData, Long> {
    
    Optional<FinancialData> findByConsentRequest(ConsentRequest consentRequest);
    
    Optional<FinancialData> findByFiRequestId(String fiRequestId);
    
    @Query("SELECT fd FROM FinancialData fd WHERE fd.isPurged = false " +
           "AND fd.dataExpiresAt < :now")
    List<FinancialData> findDataReadyForPurging(@Param("now") OffsetDateTime now);
}
