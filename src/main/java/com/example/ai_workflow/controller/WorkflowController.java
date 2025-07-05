package com.example.ai_workflow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ai_workflow.service.WorkflowOrchestratorService;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin(origins = "*")
public class WorkflowController {

    @Autowired
    private WorkflowOrchestratorService orchestratorService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startWorkflow(@RequestBody Map<String, String> payload) {
        String resourceDetails = payload.get("resourceDetails");
        if (resourceDetails == null || resourceDetails.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("log", List.of(Map.of("type", "ERROR", "title", "Input Error", "details", "Resource details cannot be empty."))));
        }
        List<Map<String, String>> log = orchestratorService.startWorkflow(resourceDetails);
        return ResponseEntity.ok(Map.of("log", log));
    }
}