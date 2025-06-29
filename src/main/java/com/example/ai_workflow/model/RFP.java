package com.example.ai_workflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class RFP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Requisition requisition;

    private String status; // e.g., DRAFT, SENT, CLOSED
    private String invitedPartners;

    public RFP(Requisition requisition) {
        this.requisition = requisition;
        this.status = "DRAFT";
    }
}