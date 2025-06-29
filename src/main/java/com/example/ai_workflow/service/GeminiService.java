package com.example.ai_workflow.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getNextAction(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(textPart));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = apiUrl + "?key=" + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // Extract the text from the complex JSON response
            Map responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("candidates")) {
                List<Map> candidates = (List<Map>) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                    List<Map> parts = (List<Map>) contentMap.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
            return "{\"action\": \"ERROR\", \"reasoning\": \"Could not parse Gemini response.\"}";
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "{\"action\": \"ERROR\", \"reasoning\": \"Failed to communicate with the AI model.\"}";
        }
    }
}