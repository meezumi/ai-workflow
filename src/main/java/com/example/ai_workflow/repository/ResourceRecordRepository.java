package com.example.ai_workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ai_workflow.model.ResourceRecord;

@Repository
public interface ResourceRecordRepository extends JpaRepository<ResourceRecord, Long> {

    /**
     * Finds all available resources whose title contains a specific keyword.
     * This is a broad search to gather all potential candidates.
     *
     * @param keyword The keyword to search for in the title (e.g., "Developer").
     * @param status The desired status (e.g., "Available").
     * @return A List of all matching ResourceRecords.
     */
    List<ResourceRecord> findByTitleContainingIgnoreCaseAndStatus(String keyword, String status);
}