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
public class ResourceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "Jane Doe"
    private String skill; // e.g., "Senior Java Developer"
    private String status; // "Available", "Assigned"

    public ResourceRecord(String name, String skill) {
        this.name = name;
        this.skill = skill;
        this.status = "Available";
    }
}