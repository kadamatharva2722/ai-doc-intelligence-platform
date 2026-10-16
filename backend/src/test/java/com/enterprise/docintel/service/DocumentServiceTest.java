package com.enterprise.docintel.service;

import com.enterprise.docintel.dto.DocumentResponse;
import com.enterprise.docintel.entity.Document;
import com.enterprise.docintel.entity.DocumentStatus;
import com.enterprise.docintel.entity.Role;
import com.enterprise.docintel.entity.User;
import com.enterprise.docintel.repository.DocumentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;

public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private com.enterprise.docintel.ai.DocumentAiService documentAiService;

    @Mock
    private com.enterprise.docintel.repository.AnalysisRepository analysisRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private User owner;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        owner = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@docintel.com")
                .role(Role.ROLE_ADMIN)
                .build();
    }

    @Test
    public void testUploadDocument_Success() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        Mockito.when(mockFile.getOriginalFilename()).thenReturn("contract.pdf");
        Mockito.when(fileStorageService.storeFile(any())).thenReturn("unique-file-path.pdf");

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .name("contract.pdf")
                .filePath("unique-file-path.pdf")
                .fileType("PDF")
                .status(DocumentStatus.PENDING)
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(documentRepository.save(any(Document.class))).thenReturn(doc);

        DocumentResponse response = documentService.uploadDocument(mockFile, owner);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("contract.pdf", response.getName());
        Assertions.assertEquals("PDF", response.getFileType());
        Assertions.assertEquals("PENDING", response.getStatus());
        Mockito.verify(kafkaProducerService, Mockito.times(1)).sendDocumentUploadedEvent(any());
    }

    @Test
    public void testSearchDocuments() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .name("contract.pdf")
                .filePath("unique-file-path.pdf")
                .fileType("PDF")
                .status(DocumentStatus.PENDING)
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(documentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(java.util.List.of(doc));

        java.util.List<DocumentResponse> results = documentService.searchDocuments(
                "contract", "PDF", "admin", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)
        );

        Assertions.assertNotNull(results);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("contract.pdf", results.get(0).getName());
        Mockito.verify(documentRepository, Mockito.times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }
}

