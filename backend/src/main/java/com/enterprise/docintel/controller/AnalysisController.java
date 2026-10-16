package com.enterprise.docintel.controller;

import com.enterprise.docintel.dto.*;
import com.enterprise.docintel.entity.User;
import com.enterprise.docintel.exception.BadRequestException;
import com.enterprise.docintel.repository.UserRepository;
import com.enterprise.docintel.service.AnalysisService;
import com.enterprise.docintel.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final DocumentService documentService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("User principal not found");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found: " + userDetails.getUsername()));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<AnalysisResponse> getAnalysis(
            @PathVariable("documentId") UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(analysisService.getAnalysisByDocumentId(documentId, user));
    }

    @PostMapping("/{documentId}/chat")
    public ResponseEntity<ChatResponse> askQuestion(
            @PathVariable("documentId") UUID documentId,
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(analysisService.askQuestion(documentId, request, user));
    }

    @GetMapping("/{documentId}/chat")
    public ResponseEntity<List<ChatResponse>> getChatHistory(
            @PathVariable("documentId") UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(analysisService.getChatHistory(documentId, user));
    }

    @PostMapping("/compare")
    public ResponseEntity<CompareResponse> compareDocuments(
            @Valid @RequestBody CompareRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(analysisService.compareDocuments(request, user));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentService.countTotalDocuments(user));
        stats.put("todayUploads", documentService.countTodayUploads(user));
        stats.put("totalAiRequests", analysisService.countTotalRequests(user));
        return ResponseEntity.ok(stats);
    }
}
