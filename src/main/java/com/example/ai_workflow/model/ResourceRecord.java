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

    private String name;       
    private String title;       
    
    @Column(length = 1024)      
    private String profile;     
    
    private String status;      

    public ResourceRecord(String name, String title, String profile) {
        this.name = name;
        this.title = title;
        this.profile = profile;
        this.status = "Available";
    }
}