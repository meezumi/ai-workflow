package com.example.ai_workflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import this

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

    @Transactional // Use a transaction to ensure data consistency
    public List<Map<String, String>> startWorkflow(String resourceDetails) {
        List<Map<String, String>> processLog = new ArrayList<>();

        // Step 1: Create the Requisition
        Requisition requisition = new Requisition(resourceDetails);
        requisitionRepository.save(requisition);
        processLog.add(Map.of("type", "USER_INPUT", "title", "Requisition Submitted", "details", "New requisition for '" + resourceDetails + "' created with ID: " + requisition.getId()));

        String currentState = "Requisition submitted for: " + resourceDetails;
        boolean workflowActive = true;
        Optional<ResourceRecord> foundResource = Optional.empty();

        while(workflowActive) {
            String prompt = buildPrompt(currentState);
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
                        processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "Searching Internal Talent Pool", "details", "Querying the database for an available resource matching '" + resourceDetails + "'."));
                        foundResource = resourceRecordRepository.findFirstBySkillContainingIgnoreCaseAndStatus(resourceDetails, "Available");
                        
                        if (foundResource.isPresent()) {
                            ResourceRecord resource = foundResource.get();
                            currentState = "An available internal resource was found: " + resource.getName() + " (" + resource.getSkill() + ").";
                            processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "Resource Found!", "details", "Success! Found an available resource: " + resource.getName() + ", a " + resource.getSkill() + "."));
                        } else {
                            currentState = "No available internal resource found matching the request.";
                            processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "No Match Found", "details", "The search of the internal database yielded no available resources."));
                        }
                        break;

                    case "ASSIGN_INTERNAL_RESOURCE":
                        if (foundResource.isPresent()) {
                            ResourceRecord resourceToAssign = foundResource.get();
                            resourceToAssign.setStatus("Assigned");
                            resourceRecordRepository.save(resourceToAssign);

                            requisition.setAssignedResource(resourceToAssign);
                            requisition.setStatus("COMPLETED");
                            requisitionRepository.save(requisition);
                            
                            processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "Resource Assigned", "details", "Resource '" + resourceToAssign.getName() + "' has been assigned to this requisition. Their status is now 'Assigned'."));
                            processLog.add(Map.of("type", "FINAL_STATUS", "title", "Workflow Complete", "details", "Requisition successfully fulfilled with an internal resource."));
                            workflowActive = false;
                        } else {
                             processLog.add(Map.of("type", "ERROR", "title", "Logic Error", "details", "AI tried to assign a resource but none was found. Proceeding to RFP."));
                             currentState = "No available internal resource found matching the request."; // Force state correction
                             // Let the loop continue to trigger the RFP path
                        }
                        break;

                    case "CREATE_RFP":
                        RFP rfp = new RFP(requisition);
                        rfpRepository.save(rfp);
                        currentState = "RFP created with ID " + rfp.getId() + ". Next step is to invite partners.";
                        processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "Create RFP", "details", "Creating a new Request for Proposal (RFP) to find an external resource. RFP ID: " + rfp.getId()));
                        break;

                    case "INVITE_PARTNERS_TO_RFP":
                        RFP existingRfp = rfpRepository.findByRequisition(requisition).orElseThrow();
                        existingRfp.setInvitedPartners("PartnerA, PartnerB, PartnerC");
                        existingRfp.setStatus("SENT");
                        rfpRepository.save(existingRfp);
                        
                        requisition.setStatus("AWAITING_PARTNER");
                        requisitionRepository.save(requisition);
                        
                        processLog.add(Map.of("type", "SYSTEM_ACTION", "title", "Invite Partners", "details", "Sending RFP to external partners. Requisition status is now AWAITING_PARTNER."));
                        processLog.add(Map.of("type", "FINAL_STATUS", "title", "Workflow Complete", "details", "The process has concluded by initiating an external search via RFP."));
                        workflowActive = false;
                        break;
                    
                    default:
                        processLog.add(Map.of("type", "ERROR", "title", "Error", "details", "AI returned an unknown or error action. Halting workflow."));
                        workflowActive = false;
                        break;
                }

            } catch (JsonProcessingException e) {
                processLog.add(Map.of("type", "ERROR", "title", "System Error", "details", "Failed to parse AI response: " + cleanJsonResponse));
                workflowActive = false;
            }
        }
        return processLog;
    }

    private String buildPrompt(String currentState) {
        // This prompt is updated to reflect the new logic
        return "You are an AI workflow orchestrator for a resource staffing agency. Your task is to decide the next step in a hiring process. " +
                "You must respond ONLY with a JSON object containing 'action' and 'reasoning'.\n\n" +
                "THE WORKFLOW:\n" +
                "1. Start -> Submit Requisition (This has already happened).\n" +
                "2. First, you MUST check for internal availability of the requested resource.\n" +
                "3. If an internal resource IS found and available -> Assign the internal resource, then the process ends.\n" +
                "4. If an internal resource IS NOT available -> Create an RFP -> Invite Partners to the RFP, then the process ends.\n\n" +
                "AVAILABLE ACTIONS IN JSON FORMAT:\n" +
                "- {\"action\": \"CHECK_AVAILABILITY\", \"reasoning\": \"The first logical step after a requisition is to check our internal talent pool.\"}\n" +
                "- {\"action\": \"ASSIGN_INTERNAL_RESOURCE\", \"reasoning\": \"An available internal resource was found, so I will assign them to fulfill the requisition.\"}\n" +
                "- {\"action\": \"CREATE_RFP\", \"reasoning\": \"No internal resources are available, so we must look for an external candidate by creating an RFP.\"}\n" +
                "- {\"action\": \"INVITE_PARTNERS_TO_RFP\", \"reasoning\": \"The RFP has been created, so the next step is to send it to our partners.\"}\n\n" +
                "CURRENT STATE: \"" + currentState + "\"\n\n" +
                "Based on the workflow and the current state, what is the next logical action? Respond with only the JSON object.";
    }
    
    // The checkInternalAvailability() method is no longer needed as its logic is now inside the main loop.
}