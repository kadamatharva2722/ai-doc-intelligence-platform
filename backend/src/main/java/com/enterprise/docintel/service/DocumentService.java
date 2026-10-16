package com.enterprise.docintel.service;

import com.enterprise.docintel.dto.DocumentResponse;
import com.enterprise.docintel.entity.Document;
import com.enterprise.docintel.entity.User;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    DocumentResponse uploadDocument(MultipartFile file, User owner) throws IOException;
    List<DocumentResponse> getAllDocuments();
    List<DocumentResponse> getUserDocuments(User owner);
    DocumentResponse getDocumentById(UUID id, User currentUser);
    Document getDocumentEntityById(UUID id, User currentUser);
    List<DocumentResponse> searchDocuments(String name, String type, String ownerUsername, LocalDateTime startDate, LocalDateTime endDate, User currentUser);
    List<DocumentResponse> naturalLanguageSearch(String query, User currentUser);
    long countTotalDocuments(User currentUser);
    long countTodayUploads(User currentUser);
}
