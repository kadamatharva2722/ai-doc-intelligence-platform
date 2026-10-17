package com.enterprise.docintel.ai.controller;

import com.enterprise.docintel.ai.service.AnalysisService;
import com.enterprise.docintel.common.dto.*;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final AnalysisService analysisService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${document.service.url:http://localhost:8082}")
    private String documentServiceUrl;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return "ROLE_EMPLOYEE";
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Claims claims) {
            String userIdStr = (String) claims.get("userId");
            if (userIdStr != null) {
                try {
                    return UUID.fromString(userIdStr);
                } catch (Exception ignore) {}
            }
        }
        return UUID.randomUUID();
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<AnalysisResponse> getAnalysis(@PathVariable("documentId") UUID documentId) {
        String username = getCurrentUsername();
        String role = getCurrentRole();
        return ResponseEntity.ok(analysisService.getAnalysisByDocumentId(documentId, username, role));
    }

    @PostMapping("/{documentId}/chat")
    public ResponseEntity<ChatResponse> askQuestion(
            @PathVariable("documentId") UUID documentId,
            @Valid @RequestBody ChatRequest request) {
        String username = getCurrentUsername();
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(analysisService.askQuestion(documentId, request, userId, username));
    }

    @GetMapping("/{documentId}/chat")
    public ResponseEntity<List<ChatResponse>> getChatHistory(@PathVariable("documentId") UUID documentId) {
        return ResponseEntity.ok(analysisService.getChatHistory(documentId));
    }

    @PostMapping("/compare")
    public ResponseEntity<CompareResponse> compareDocuments(@Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(analysisService.compareDocuments(request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        String username = getCurrentUsername();
        String role = getCurrentRole();

        Map<String, Object> stats = new HashMap<>();
        long totalAiRequests = analysisService.countTotalRequests(username, role);
        stats.put("totalAiRequests", totalAiRequests);

        // Fetch document stats from document-service
        try {
            String url = documentServiceUrl + "/api/documents/stats?username=" + username + "&role=" + role;
            ResponseEntity<Map<String, Object>> docStats = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (docStats.getBody() != null) {
                stats.put("totalDocuments", docStats.getBody().getOrDefault("totalDocuments", 0));
                stats.put("todayUploads", docStats.getBody().getOrDefault("todayUploads", 0));
            } else {
                stats.put("totalDocuments", 0);
                stats.put("todayUploads", 0);
            }
        } catch (Exception ex) {
            log.warn("Could not retrieve document stats from document-service: {}", ex.getMessage());
            stats.put("totalDocuments", 0);
            stats.put("todayUploads", 0);
        }

        return ResponseEntity.ok(stats);
    }
}
