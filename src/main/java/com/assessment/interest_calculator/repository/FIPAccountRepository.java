package com.assessment.interest_calculator.repository;

import com.assessment.interest_calculator.entity.FIPAccount;
import com.assessment.interest_calculator.entity.FinancialData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FIPAccountRepository extends JpaRepository<FIPAccount, Long> {
    
    List<FIPAccount> findByFinancialData(FinancialData financialData);
    
    List<FIPAccount> findByFipId(String fipId);
    
    List<FIPAccount> findByFiType(String fiType);
    
    @Query("SELECT fa FROM FIPAccount fa WHERE fa.financialData.id = :financialDataId")
    List<FIPAccount> findByFinancialDataId(@Param("financialDataId") Long financialDataId);
}
