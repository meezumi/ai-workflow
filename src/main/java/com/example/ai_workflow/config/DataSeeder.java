package com.example.ai_workflow.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.ai_workflow.model.ResourceRecord;
import com.example.ai_workflow.repository.ResourceRecordRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ResourceRecordRepository resourceRecordRepository;

    @Override
    public void run(String... args) throws Exception {
        // Clear existing records to ensure a fresh start
        resourceRecordRepository.deleteAll();

        // Add some sample resources to our internal talent pool
        resourceRecordRepository.save(new ResourceRecord("Alice Johnson", "Senior Java Developer"));
        resourceRecordRepository.save(new ResourceRecord("Bob Williams", "React Frontend Developer"));
        resourceRecordRepository.save(new ResourceRecord("Charlie Brown", "DevOps Engineer"));
        resourceRecordRepository.save(new ResourceRecord("Diana Prince", "Project Manager"));

        System.out.println("Database seeded with 4 available resources.");
    }
}