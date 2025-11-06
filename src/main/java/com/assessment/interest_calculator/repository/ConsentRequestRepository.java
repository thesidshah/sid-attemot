package com.assessment.interest_calculator.repository;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.assessment.interest_calculator.entity.ConsentRequest;

@Repository
public interface ConsentRequestRepository extends JpaRepository<ConsentRequest, Long> {
    /**
     * Find a ConsentRequest for its status by its unique requestId.
     */
    Optional<ConsentRequest> findByRequestId(String requestId);

    /**
     * Find a ConsentRequest by customer reference ID. This will not be using the third party APIs but the database lookup.
     */
    @Query("SELECT cr FROM ConsentRequest cr WHERE cr.customerRefId = :customerRefId")
    Optional<ConsentRequest> findByCustomerRefId(String customerRefId);

    /**
     * TODO: Implement this if FI is collected.
     */
    // Optional<ConsentRequest> listByFiRequestId(String fiRequestId);

    /**
     * Find all ConsentRequests by their status.
     */
    @Query("SELECT cr FROM ConsentRequest cr WHERE cr.status = :status")
    List<ConsentRequest> findAllByStatus(String status);
    

}
