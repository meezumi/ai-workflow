package com.example.ai_workflow.service;

import com.example.ai_workflow.model.*;
import com.example.ai_workflow.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

@Service
public class WorkflowOrchestratorService {

    @Autowired private RequisitionRepository requisitionRepository;
    @Autowired private ResourceRecordRepository resourceRecordRepository;
    @Autowired private RFPRepository rfpRepository;
    @Autowired private GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String startWorkflow(String resourceDetails) {
        StringBuilder processLog = new StringBuilder();

        // 1. Initial Step: Create a Requisition
        Requisition requisition = new Requisition(resourceDetails);
        requisitionRepository.save(requisition);
        processLog.append("Step 1: User submitted requisition for '").append(resourceDetails).append("'. Record created with ID: ").append(requisition.getId()).append("\n\n");

        String currentState = "Requisition submitted for " + resourceDetails;
        boolean workflowActive = true;

        while(workflowActive) {
            String prompt = buildPrompt(currentState);
            String aiResponse = geminiService.getNextAction(prompt);

            // Clean up the AI response which might be wrapped in ```json ... ```
            String cleanJsonResponse = aiResponse.replace("```json", "").replace("```", "").trim();

            try {
                Map<String, String> command = objectMapper.readValue(cleanJsonResponse, Map.class);
                String action = command.get("action");
                String reasoning = command.get("reasoning");

                processLog.append("AI Decision: ").append(reasoning).append("\n");
                processLog.append("AI Action: ").append(action).append("\n");

                switch (action) {
                    case "CHECK_AVAILABILITY":
                        boolean isAvailable = checkInternalAvailability(); // Simulated API call
                        currentState = isAvailable ? "Resource is available" : "Resource is not available";
                        processLog.append("System Action: Checking internal resource availability... Result: ").append(currentState).append("\n\n");
                        break;

                    case "CREATE_RESOURCE_RECORD":
                        ResourceRecord record = new ResourceRecord(requisition);
                        resourceRecordRepository.save(record);
                        requisition.setStatus("COMPLETED");
                        requisitionRepository.save(requisition);
                        currentState = "Internal resource record created. Workflow finished.";
                        processLog.append("System Action: Creating internal resource record in database. Requisition ID ").append(requisition.getId()).append(" is now COMPLETED.\n\n");
                        workflowActive = false; // End of this path
                        break;

                    case "CREATE_RFP":
                        RFP rfp = new RFP(requisition);
                        rfpRepository.save(rfp);
                        currentState = "RFP created with ID " + rfp.getId() + ". Next step is to invite partners.";
                        processLog.append("System Action: Creating new RFP in database. RFP ID: ").append(rfp.getId()).append("\n\n");
                        break;

                    case "INVITE_PARTNERS_TO_RFP":
                        RFP existingRfp = rfpRepository.findByRequisition(requisition).orElseThrow();
                        existingRfp.setInvitedPartners("PartnerA, PartnerB, PartnerC");
                        existingRfp.setStatus("SENT");
                        rfpRepository.save(existingRfp);
                        requisition.setStatus("AWAITING_PARTNER");
                        requisitionRepository.save(requisition);
                        currentState = "Partners invited to RFP. Workflow finished.";
                        processLog.append("System Action: Sending RFP to partners. Requisition ID ").append(requisition.getId()).append(" is now AWAITING_PARTNER.\n\n");
                        workflowActive = false; // End of this path
                        break;
                    
                    case "END_WORKFLOW":
                         processLog.append("System Action: AI has determined the workflow is complete.\n\n");
                         workflowActive = false;
                         break;

                    default:
                        processLog.append("System Action: AI returned an unknown or error action. Halting workflow.\n\n");
                        workflowActive = false;
                        break;
                }

            } catch (JsonProcessingException e) {
                processLog.append("System ERROR: Failed to parse AI response: ").append(cleanJsonResponse).append("\n\n");
                workflowActive = false;
            }
        }
        return processLog.toString();
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