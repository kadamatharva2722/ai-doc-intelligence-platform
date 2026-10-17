package com.enterprise.docintel.document.controller;

import com.enterprise.docintel.common.dto.DocumentResponse;
import com.enterprise.docintel.common.entity.DocumentStatus;
import com.enterprise.docintel.common.exception.BadRequestException;
import com.enterprise.docintel.document.service.DocumentService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file) throws IOException {
        String username = getCurrentUsername();
        UUID userId = getCurrentUserId();
        log.info("Document upload requested by user '{}' ({})", username, userId);
        DocumentResponse response = documentService.uploadDocument(file, userId, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        String role = getCurrentRole();
        String username = getCurrentUsername();
        if ("ROLE_ADMIN".equals(role)) {
            return ResponseEntity.ok(documentService.getAllDocuments());
        }
        return ResponseEntity.ok(documentService.getUserDocuments(username));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponse>> searchDocuments(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "owner", required = false) String ownerUsername,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        String role = getCurrentRole();
        String username = getCurrentUsername();
        return ResponseEntity.ok(documentService.searchDocuments(name, type, ownerUsername, startDate, endDate, username, role));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDocumentStats(
            @RequestParam(value = "username", required = false) String paramUsername,
            @RequestParam(value = "role", required = false) String paramRole) {
        String username = paramUsername != null ? paramUsername : getCurrentUsername();
        String role = paramRole != null ? paramRole : getCurrentRole();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentService.countTotalDocuments(username, role));
        stats.put("todayUploads", documentService.countTodayUploads(username, role));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/nl-search")
    public ResponseEntity<List<DocumentResponse>> naturalLanguageSearch(@RequestParam("query") String query) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Query parameter cannot be empty");
        }
        String username = getCurrentUsername();
        String role = getCurrentRole();
        List<DocumentResponse> results = documentService.searchDocuments(query.trim(), null, null, null, null, username, role);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable("id") UUID id) {
        String username = getCurrentUsername();
        String role = getCurrentRole();
        return ResponseEntity.ok(documentService.getDocumentById(id, username, role));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable("id") UUID id, @RequestParam("status") DocumentStatus status) {
        documentService.updateDocumentStatus(id, status);
        return ResponseEntity.noContent().build();
    }
}
