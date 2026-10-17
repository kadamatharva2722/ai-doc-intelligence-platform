package com.enterprise.docintel.document.service;

import com.enterprise.docintel.common.dto.DocumentResponse;
import com.enterprise.docintel.common.dto.DocumentUploadedEvent;
import com.enterprise.docintel.common.entity.DocumentStatus;
import com.enterprise.docintel.common.exception.ResourceNotFoundException;
import com.enterprise.docintel.document.entity.Document;
import com.enterprise.docintel.document.kafka.DocumentEventProducer;
import com.enterprise.docintel.document.repository.DocumentRepository;
import com.enterprise.docintel.document.repository.DocumentSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentEventProducer documentEventProducer;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               FileStorageService fileStorageService,
                               DocumentEventProducer documentEventProducer) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.documentEventProducer = documentEventProducer;
    }

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, UUID ownerId, String ownerUsername) throws IOException {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new IllegalArgumentException("File name must not be null");
        }

        String fileType = "";
        int extIndex = originalFileName.lastIndexOf('.');
        if (extIndex > 0) {
            fileType = originalFileName.substring(extIndex + 1).toUpperCase();
        }

        if (!List.of("PDF", "DOCX", "TXT").contains(fileType)) {
            throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }

        String uniqueFileName = fileStorageService.storeFile(file);

        Document document = Document.builder()
                .name(originalFileName)
                .filePath(uniqueFileName)
                .fileType(fileType)
                .status(DocumentStatus.PENDING)
                .ownerId(ownerId)
                .ownerUsername(ownerUsername)
                .build();

        Document savedDoc = documentRepository.save(document);
        log.info("Document saved: {} with ID: {}", savedDoc.getName(), savedDoc.getId());

        // Publish to Kafka topic for async analysis
        DocumentUploadedEvent event = DocumentUploadedEvent.builder()
                .documentId(savedDoc.getId())
                .name(savedDoc.getName())
                .filePath(savedDoc.getFilePath())
                .fileType(savedDoc.getFileType())
                .ownerUsername(savedDoc.getOwnerUsername())
                .ownerId(savedDoc.getOwnerId())
                .build();

        documentEventProducer.sendDocumentUploadedEvent(event);

        return mapToResponse(savedDoc);
    }

    @Override
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getUserDocuments(String ownerUsername) {
        return documentRepository.findByOwnerUsername(ownerUsername).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse getDocumentById(UUID id, String currentUser, String currentRole) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        if (currentUser != null && !"ROLE_ADMIN".equals(currentRole) && !doc.getOwnerUsername().equalsIgnoreCase(currentUser)) {
            throw new ResourceNotFoundException("Document not found with ID: " + id);
        }

        return mapToResponse(doc);
    }

    @Override
    public List<DocumentResponse> searchDocuments(String name, String type, String ownerUsername, LocalDateTime startDate, LocalDateTime endDate, String currentUser, String currentRole) {
        String effectiveOwner = (currentUser != null && !"ROLE_ADMIN".equals(currentRole))
                ? currentUser
                : ownerUsername;

        Specification<Document> spec = Specification.allOf(
                DocumentSpecifications.hasNameLike(name),
                DocumentSpecifications.hasFileType(type),
                DocumentSpecifications.hasOwnerUsernameLike(effectiveOwner),
                DocumentSpecifications.createdAfterOrEqual(startDate),
                DocumentSpecifications.createdBeforeOrEqual(endDate)
        );

        return documentRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countTotalDocuments(String currentUser, String currentRole) {
        if (currentUser != null && !"ROLE_ADMIN".equals(currentRole)) {
            return documentRepository.countByOwnerUsername(currentUser);
        }
        return documentRepository.count();
    }

    @Override
    public long countTodayUploads(String currentUser, String currentRole) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        if (currentUser != null && !"ROLE_ADMIN".equals(currentRole)) {
            return documentRepository.countByOwnerUsernameAndCreatedAtAfter(currentUser, startOfDay);
        }
        return documentRepository.countByCreatedAtAfter(startOfDay);
    }

    @Override
    public void updateDocumentStatus(UUID documentId, DocumentStatus status) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
        doc.setStatus(status);
        documentRepository.save(doc);
    }

    private DocumentResponse mapToResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .fileType(doc.getFileType())
                .status(doc.getStatus().name())
                .ownerUsername(doc.getOwnerUsername())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
