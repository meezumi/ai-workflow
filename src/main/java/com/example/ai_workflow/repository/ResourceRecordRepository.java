package com.example.ai_workflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ai_workflow.model.ResourceRecord;

@Repository
public interface ResourceRecordRepository extends JpaRepository<ResourceRecord, Long> {

    /**
     * Finds the first available resource whose skill contains the requested skill string.
     * This allows a search for "Java Developer" to match a resource with skill "Senior Java Developer".
     *
     * @param skill The skill to search for.
     * @param status The desired status (e.g., "Available").
     * @return An Optional containing the found ResourceRecord, or empty if none found.
     */
    Optional<ResourceRecord> findFirstBySkillContainingIgnoreCaseAndStatus(String skill, String status);
}