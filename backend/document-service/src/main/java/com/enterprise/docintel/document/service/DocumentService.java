package com.enterprise.docintel.document.service;

import com.enterprise.docintel.common.dto.DocumentResponse;
import com.enterprise.docintel.common.entity.DocumentStatus;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    DocumentResponse uploadDocument(MultipartFile file, UUID ownerId, String ownerUsername) throws IOException;
    List<DocumentResponse> getAllDocuments();
    List<DocumentResponse> getUserDocuments(String ownerUsername);
    DocumentResponse getDocumentById(UUID id, String currentUser, String currentRole);
    List<DocumentResponse> searchDocuments(String name, String type, String ownerUsername, LocalDateTime startDate, LocalDateTime endDate, String currentUser, String currentRole);
    long countTotalDocuments(String currentUser, String currentRole);
    long countTodayUploads(String currentUser, String currentRole);
    void updateDocumentStatus(UUID documentId, DocumentStatus status);
}
