package com.example.ai_workflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Requisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String resourceDetails;
    private String status; // e.g., PENDING, PROCESSING, COMPLETED

    public Requisition(String resourceDetails) {
        this.resourceDetails = resourceDetails;
        this.status = "PENDING";
    }
}