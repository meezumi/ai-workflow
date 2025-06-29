package com.example.ai_workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ai_workflow.model.Requisition;

@Repository
public interface RequisitionRepository extends JpaRepository<Requisition, Long> {}
