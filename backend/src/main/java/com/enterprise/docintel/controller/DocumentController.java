package com.enterprise.docintel.controller;

import com.enterprise.docintel.dto.DocumentResponse;
import com.enterprise.docintel.entity.User;
import com.enterprise.docintel.exception.BadRequestException;
import com.enterprise.docintel.repository.UserRepository;
import com.enterprise.docintel.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("User principal not found");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found: " + userDetails.getUsername()));
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        User owner = getAuthenticatedUser(userDetails);
        DocumentResponse response = documentService.uploadDocument(file, owner);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user.getRole() == com.enterprise.docintel.entity.Role.ROLE_ADMIN) {
            return ResponseEntity.ok(documentService.getAllDocuments());
        }
        return ResponseEntity.ok(documentService.getUserDocuments(user));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponse>> searchDocuments(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "owner", required = false) String ownerUsername,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(documentService.searchDocuments(name, type, ownerUsername, startDate, endDate, user));
    }

    @GetMapping("/nl-search")
    public ResponseEntity<List<DocumentResponse>> naturalLanguageSearch(
            @RequestParam("query") String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Query parameter cannot be empty");
        }
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(documentService.naturalLanguageSearch(query, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(documentService.getDocumentById(id, user));
    }
}
