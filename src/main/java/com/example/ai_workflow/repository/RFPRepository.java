package com.example.ai_workflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ai_workflow.model.RFP;
import com.example.ai_workflow.model.Requisition;

@Repository
public interface RFPRepository extends JpaRepository<RFP, Long> {

    /**
     * Finds an RFP entity by its associated Requisition.
     * Spring Data JPA automatically creates the query from the method name "findByRequisition".
     *
     * @param requisition The requisition to search for.
     * @return An Optional containing the found RFP, or an empty Optional if none is found.
     */
    Optional<RFP> findByRequisition(Requisition requisition);
}