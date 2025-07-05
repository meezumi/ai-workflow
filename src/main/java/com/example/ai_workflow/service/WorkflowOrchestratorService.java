package com.example.ai_workflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import this

import com.example.ai_workflow.model.Requisition;
import com.example.ai_workflow.model.ResourceRecord;
import com.example.ai_workflow.repository.RFPRepository;
import com.example.ai_workflow.repository.RequisitionRepository;
import com.example.ai_workflow.repository.ResourceRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WorkflowOrchestratorService {

    @Autowired private RequisitionRepository requisitionRepository;
    @Autowired private ResourceRecordRepository resourceRecordRepository;
    @Autowired private RFPRepository rfpRepository;
    @Autowired private GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<Map<String, String>> startWorkflow(String resourceDetails) {
        List<Map<String, String>> processLog = new ArrayList<>();
        Requisition requisition = new Requisition(resourceDetails);
        requisitionRepository.save(requisition);
        processLog.add(Map.of("type", "USER_INPUT", "title", "Requisition Submitted", "details", "New requisition for '" + resourceDetails + "' created with ID: " + requisition.getId()));

        String currentState = "Requisition submitted for: " + resourceDetails;
        boolean workflowActive = true;
        List<ResourceRecord> candidatePool = new ArrayList<>();
        ResourceRecord chosenResource = null;

        while(workflowActive) {
            String prompt = buildMainPrompt(currentState);
            String aiResponse = geminiService.getNextAction(prompt);
            String cleanJsonResponse = aiResponse.replace("```json", "").replace("```", "").trim();

            try {
                Map<String, String> command = objectMapper.readValue(cleanJsonResponse, Map.class);
                String action = command.get("action");
                String reasoning = command.get("reasoning");

                processLog.add(Map.of("type", "AI_DECISION", "title", "AI Thought Process", "details", reasoning));
                processLog.add(Map.of("type", "AI_ACTION", "title", "AI Command", "details", "Instructed action: " + action));

                switch (action) {
                    case "CHECK_AVAILABILITY":
                        // Broad search for "developer" to create a candidate pool
                        candidatePool = resourceRecordRepository.findByTitleContainingIgnoreCaseAndStatus("Developer", "Available");
                        
                        if (candidatePool.isEmpty()) {
                            currentState = "No internal developers are available.";
                            processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "No Candidates Found", "details", "The search of the internal database yielded no available developers."));
                        } else if (candidatePool.size() == 1) {
                            chosenResource = candidatePool.get(0);
                            currentState = "Exactly one suitable internal resource was found: " + chosenResource.getName();
                            processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "One Candidate Found", "details", "Found one matching resource: " + chosenResource.getName() + " (" + chosenResource.getTitle() + "). Proceeding to assignment."));
                        } else {
                            currentState = "Multiple (" + candidatePool.size() + ") potential candidates were found. The best match must be selected.";
                            String candidateNames = candidatePool.stream().map(r -> r.getName() + " (" + r.getTitle() + ")").collect(Collectors.joining(", "));
                            processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "Multiple Candidates Found", "details", "Found " + candidatePool.size() + " potential candidates: " + candidateNames));
                        }
                        break;
                    
                    case "SELECT_BEST_CANDIDATE":
                        String selectionPrompt = buildSelectionPrompt(resourceDetails, candidatePool);
                        String selectionResponse = geminiService.getNextAction(selectionPrompt);
                        String cleanSelectionResponse = selectionResponse.replace("```json", "").replace("```", "").trim();
                        
                        Map<String, Object> selectionResult = objectMapper.readValue(cleanSelectionResponse, new TypeReference<>() {});
                        Long bestCandidateId = Long.parseLong(selectionResult.get("best_candidate_id").toString());
                        String selectionReasoning = (String) selectionResult.get("reasoning");
                        
                        chosenResource = resourceRecordRepository.findById(bestCandidateId).orElse(null);

                        if(chosenResource != null) {
                            currentState = "AI has selected '" + chosenResource.getName() + "' as the best fit.";
                            processLog.add(Map.of("type", "AI_DECISION", "title", "AI Selection Made", "details", "The AI selected " + chosenResource.getName() + ". Justification: '" + selectionReasoning + "'"));
                        } else {
                            currentState = "AI selection failed, could not find candidate with ID " + bestCandidateId + ". Proceeding to RFP.";
                            processLog.add(Map.of("type", "ERROR", "title", "AI Selection Failed", "details", "AI returned an invalid candidate ID. Manual intervention may be required."));
                        }
                        break;

                    case "ASSIGN_INTERNAL_RESOURCE":
                        if (chosenResource != null) {
                            chosenResource.setStatus("Assigned");
                            resourceRecordRepository.save(chosenResource);
                            requisition.setAssignedResource(chosenResource);
                            requisition.setStatus("COMPLETED");
                            requisitionRepository.save(requisition);
                            
                            processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "Resource Assigned", "details", "Resource '" + chosenResource.getName() + "' has been assigned to this requisition."));
                            processLog.add(Map.of("type", "FINAL_STATUS", "title", "Workflow Complete", "details", "Requisition successfully fulfilled with an internal resource."));
                            workflowActive = false;
                        } else {
                             processLog.add(Map.of("type", "ERROR", "title", "Logic Error", "details", "AI tried to assign a resource but no resource was chosen. Halting."));
                             workflowActive = false;
                        }
                        break;
                    
                    // ... CREATE_RFP and INVITE_PARTNERS_TO_RFP cases remain the same ...
                }
            } catch (Exception e) { // Catch broader exceptions
                processLog.add(Map.of("type", "ERROR", "title", "System Error", "details", "An unexpected error occurred: " + e.getMessage()));
                workflowActive = false;
            }
        }
        return processLog;
    }
    
    private String buildSelectionPrompt(String userRequest, List<ResourceRecord> candidates) {
        // Convert candidates list to a JSON string for the prompt
        String candidatesJson = candidates.stream()
            .map(c -> String.format("{\"id\": %d, \"name\": \"%s\", \"title\": \"%s\", \"profile\": \"%s\"}",
                c.getId(), c.getName(), c.getTitle(), c.getProfile().replace("\"", "'")))
            .collect(Collectors.joining(",\n", "[\n", "\n]"));

        return "You are an expert HR recruitment manager. Your task is to select the single best candidate from a list to fulfill a specific user request. " +
                "Analyze the user's request carefully against each candidate's profile.\n\n" +
                "USER REQUEST:\n\"" + userRequest + "\"\n\n" +
                "CANDIDATES (in JSON format):\n" + candidatesJson + "\n\n" +
                "Based on this information, which candidate is the most suitable? " +
                "You MUST respond with ONLY a JSON object with two keys: 'best_candidate_id' (the integer ID of your chosen candidate) and 'reasoning' (a brief explanation for your choice).\n\n" +
                "Example response format: {\"best_candidate_id\": 1, \"reasoning\": \"Alice Johnson is the best fit due to her extensive experience with microservices, which was a key part of the request.\"}\n\n" +
                "Provide your response now.";
    }

    private String buildMainPrompt(String currentState) {
        return "You are an AI workflow orchestrator for a resource staffing agency. Your task is to decide the next step in a hiring process. " +
                "You must respond ONLY with a JSON object containing 'action' and 'reasoning'.\n\n" +
                "THE WORKFLOW:\n" +
                "1. First, CHECK_AVAILABILITY of all internal resources.\n" +
                "2. If multiple candidates are found -> SELECT_BEST_CANDIDATE to have the AI choose the most suitable one.\n" +
                "3. If one or zero candidates are found, OR after a selection is made -> ASSIGN_INTERNAL_RESOURCE if a suitable candidate exists.\n" +
                "4. If no suitable internal resource exists at any point -> CREATE_RFP to look externally.\n" +
                "5. After an RFP is created -> INVITE_PARTNERS_TO_RFP.\n\n" +
                "AVAILABLE ACTIONS:\n" +
                "- {\"action\": \"CHECK_AVAILABILITY\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"SELECT_BEST_CANDIDATE\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"ASSIGN_INTERNAL_RESOURCE\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"CREATE_RFP\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"INVITE_PARTNERS_TO_RFP\", \"reasoning\": \"...\"}\n\n" +
                "CURRENT STATE: \"" + currentState + "\"\n\n" +
                "Based on the workflow and the current state, what is the next logical action? Respond with only the JSON object.";
    }
}