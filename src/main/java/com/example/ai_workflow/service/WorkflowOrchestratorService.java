package com.example.ai_workflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.ai_workflow.model.RFP;
import com.example.ai_workflow.model.Requisition;
import com.example.ai_workflow.model.ResourceRecord;
import com.example.ai_workflow.repository.RFPRepository;
import com.example.ai_workflow.repository.RequisitionRepository;
import com.example.ai_workflow.repository.ResourceRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class WorkflowOrchestratorService {

    @Autowired private RequisitionRepository requisitionRepository;
    @Autowired private ResourceRecordRepository resourceRecordRepository;
    @Autowired private RFPRepository rfpRepository;
    @Autowired private GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, String>> startWorkflow(String resourceDetails) {
        List<Map<String, String>> processLog = new ArrayList<>();

        // 1. Initial Step: Create a Requisition
        Requisition requisition = new Requisition(resourceDetails);
        requisitionRepository.save(requisition);
        processLog.add(Map.of(
            "type", "USER_INPUT",
            "title", "Step 1: Requisition Submitted",
            "details", "User submitted a requisition for '" + resourceDetails + "'. A new record was created with ID: " + requisition.getId()
        ));

        String currentState = "Requisition submitted for " + resourceDetails;
        boolean workflowActive = true;

        while(workflowActive) {
            String prompt = buildPrompt(currentState);
            String aiResponse = geminiService.getNextAction(prompt);
            String cleanJsonResponse = aiResponse.replace("```json", "").replace("```", "").trim();

            try {
                Map<String, String> command = objectMapper.readValue(cleanJsonResponse, Map.class);
                String action = command.get("action");
                String reasoning = command.get("reasoning");

                processLog.add(Map.of(
                    "type", "AI_DECISION",
                    "title", "AI Thought Process",
                    "details", reasoning
                ));
                 processLog.add(Map.of(
                    "type", "AI_ACTION",
                    "title", "AI Command",
                    "details", "Next instructed action: " + action
                ));

                switch (action) {
                    case "CHECK_AVAILABILITY":
                        boolean isAvailable = checkInternalAvailability();
                        currentState = isAvailable ? "Resource is available" : "Resource is not available";
                        processLog.add(Map.of(
                            "type", "SYSTEM_ACTION",
                            "title", "Step 2: Check Availability",
                            "details", "System checked internal resource availability... Result: " + (isAvailable ? "Available" : "Not Available")
                        ));
                        break;

                    case "CREATE_RESOURCE_RECORD":
                        ResourceRecord record = new ResourceRecord(requisition);
                        resourceRecordRepository.save(record);
                        requisition.setStatus("COMPLETED");
                        requisitionRepository.save(requisition);
                        currentState = "Internal resource record created. Workflow finished.";
                        processLog.add(Map.of(
                            "type", "SYSTEM_ACTION",
                            "title", "Step 3: Create Resource Record",
                            "details", "Created internal resource record in database. Requisition ID " + requisition.getId() + " is now COMPLETED."
                        ));
                         processLog.add(Map.of(
                            "type", "FINAL_STATUS",
                            "title", "Workflow Complete",
                            "details", "The process concluded successfully by allocating an internal resource."
                        ));
                        workflowActive = false;
                        break;

                    case "CREATE_RFP":
                        RFP rfp = new RFP(requisition);
                        rfpRepository.save(rfp);
                        currentState = "RFP created with ID " + rfp.getId() + ". Next step is to invite partners.";
                        processLog.add(Map.of(
                            "type", "SYSTEM_ACTION",
                            "title", "Step 3: Create RFP",
                            "details", "Resource not available internally. Creating a new Request for Proposal (RFP) in the database. RFP ID: " + rfp.getId()
                        ));
                        break;

                    case "INVITE_PARTNERS_TO_RFP":
                        RFP existingRfp = rfpRepository.findByRequisition(requisition).orElseThrow();
                        existingRfp.setInvitedPartners("PartnerA, PartnerB, PartnerC");
                        existingRfp.setStatus("SENT");
                        rfpRepository.save(existingRfp);
                        requisition.setStatus("AWAITING_PARTNER");
                        requisitionRepository.save(requisition);
                        currentState = "Partners invited to RFP. Workflow finished.";
                         processLog.add(Map.of(
                            "type", "SYSTEM_ACTION",
                            "title", "Step 4: Invite Partners",
                            "details", "Sending RFP to external partners. Requisition ID " + requisition.getId() + " is now AWAITING_PARTNER."
                        ));
                         processLog.add(Map.of(
                            "type", "FINAL_STATUS",
                            "title", "Workflow Complete",
                            "details", "The process has concluded by initiating an external search via RFP."
                        ));
                        workflowActive = false;
                        break;
                    
                    case "END_WORKFLOW":
                        processLog.add(Map.of(
                            "type", "FINAL_STATUS",
                            "title", "Workflow Ended by AI",
                            "details", "The AI has determined the workflow is complete at its current stage."
                        ));
                         workflowActive = false;
                         break;

                    default:
                        processLog.add(Map.of(
                            "type", "ERROR",
                            "title", "Error",
                            "details", "AI returned an unknown or error action. Halting workflow."
                        ));
                        workflowActive = false;
                        break;
                }

            } catch (JsonProcessingException e) {
                processLog.add(Map.of(
                    "type", "ERROR",
                    "title", "System Error",
                    "details", "Failed to parse AI response: " + cleanJsonResponse
                ));
                workflowActive = false;
            }
        }
        return processLog;
    }

    private String buildPrompt(String currentState) {
        return "You are an AI workflow orchestrator. Your task is to decide the next step in a business process based on a defined flowchart and the current state. " +
                "You must respond ONLY with a JSON object containing 'action' and 'reasoning'.\n\n" +
                "THE WORKFLOW:\n" +
                "1. Start -> Submit Requisition (This has already happened).\n" +
                "2. Check Availability - Hit API: After a requisition is submitted, you must check for internal availability.\n" +
                "3. If 'Available' -> Create Resource Record, then the process ends.\n" +
                "4. If 'Not Available' -> Create RFP -> Invite Partners to participate in RFP, then the process ends.\n\n" +
                "AVAILABLE ACTIONS IN JSON FORMAT:\n" +
                "- {\"action\": \"CHECK_AVAILABILITY\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"CREATE_RESOURCE_RECORD\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"CREATE_RFP\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"INVITE_PARTNERS_TO_RFP\", \"reasoning\": \"...\"}\n" +
                "- {\"action\": \"END_WORKFLOW\", \"reasoning\": \"The process has reached a logical conclusion.\"}\n\n" +
                "CURRENT STATE: \"" + currentState + "\"\n\n" +
                "Based on the workflow and the current state, what is the next action to take? Respond with only the JSON object.";
    }

    // This simulates an external API call to check for resource availability.
    private boolean checkInternalAvailability() {
        return new Random().nextBoolean();
    }
}