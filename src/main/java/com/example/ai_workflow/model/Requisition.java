package com.example.ai_workflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Requisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String resourceDetails; // The user's request, e.g., "Java Developer"
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    // Link to the resource that fulfilled this request
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_resource_id")
    private ResourceRecord assignedResource;

    public Requisition(String resourceDetails) {
        this.resourceDetails = resourceDetails;
        this.status = "PENDING";
    }
}