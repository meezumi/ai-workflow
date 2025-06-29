package com.example.ai_workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ai_workflow.model.ResourceRecord;

@Repository
public interface ResourceRecordRepository extends JpaRepository<ResourceRecord, Long> {
    // Spring Data JPA provides all necessary methods like save(), findById(), findAll(), etc.
    // No additional methods are needed for this specific workflow.
}