package com.example.ai_workflow.model;

import jakarta.persistence.Column;
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

    private String name;        // e.g., "Alice Johnson"
    private String title;       // e.g., "Senior Java Developer"
    
    @Column(length = 1024)      // A longer field for a detailed profile
    private String profile;     // e.g., "10+ years experience with Java, Spring Boot, Microservices architecture..."
    
    private String status;      // "Available", "Assigned"

    public ResourceRecord(String name, String title, String profile) {
        this.name = name;
        this.title = title;
        this.profile = profile;
        this.status = "Available";
    }
}