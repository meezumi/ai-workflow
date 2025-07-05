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
        resourceRecordRepository.deleteAll();

        resourceRecordRepository.save(new ResourceRecord("Alice Johnson", "Senior Java Developer",
            "10+ years of experience in backend development with Java and Spring Boot. Expert in microservices architecture, REST APIs, and PostgreSQL. Proficient with AWS services (EC2, S3, RDS). Strong leadership skills."));
        
        resourceRecordRepository.save(new ResourceRecord("Mark Lee", "Mid-Level Java Developer",
            "4 years of experience with Java and Spring. Good understanding of microservices and cloud deployments on AWS. Eager to learn and contribute to complex projects. Familiar with Docker and Kubernetes."));

        resourceRecordRepository.save(new ResourceRecord("Bob Williams", "Senior React Developer",
            "8 years in frontend development, specializing in React, Redux, and TypeScript. Builds responsive and performant user interfaces. Works closely with UX/UI designers."));
        
        resourceRecordRepository.save(new ResourceRecord("Chloe Davis", "Junior Java Developer",
            "1 year of experience after graduating with a degree in Computer Science. Strong foundation in Java and object-oriented principles. Has completed several projects using Spring Boot and Hibernate."));

        System.out.println("Database seeded with 4 detailed resource profiles.");
    }
}