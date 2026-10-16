package com.enterprise.docintel.service;

import com.enterprise.docintel.dto.DocumentResponse;
import com.enterprise.docintel.entity.Document;
import com.enterprise.docintel.entity.DocumentStatus;
import com.enterprise.docintel.entity.Role;
import com.enterprise.docintel.entity.User;
import com.enterprise.docintel.exception.ResourceNotFoundException;
import com.enterprise.docintel.repository.DocumentRepository;
import com.enterprise.docintel.ai.DocumentAiService;
import com.enterprise.docintel.entity.DocumentAnalysis;
import com.enterprise.docintel.repository.AnalysisRepository;
import com.enterprise.docintel.repository.DocumentSpecifications;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final KafkaProducerService kafkaProducerService;
    private final DocumentAiService documentAiService;
    private final AnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, User owner) throws IOException {
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
                .owner(owner)
                .build();

        Document savedDoc = documentRepository.save(document);
        log.info("Document saved: {} with ID: {}", savedDoc.getName(), savedDoc.getId());

        // Publish to Kafka topic for async analysis
        kafkaProducerService.sendDocumentUploadedEvent(savedDoc.getId());

        return mapToResponse(savedDoc);
    }

    @Override
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getUserDocuments(User owner) {
        return documentRepository.findByOwner(owner).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse getDocumentById(UUID id, User currentUser) {
        return mapToResponse(getDocumentEntityById(id, currentUser));
    }

    @Override
    public Document getDocumentEntityById(UUID id, User currentUser) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        if (currentUser != null && !currentUser.getRole().equals(Role.ROLE_ADMIN) && !doc.getOwner().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Document not found with ID: " + id);
        }

        return doc;
    }

    @Override
    public List<DocumentResponse> searchDocuments(String name, String type, String ownerUsername, LocalDateTime startDate, LocalDateTime endDate, User currentUser) {
        String effectiveOwner = (currentUser != null && !currentUser.getRole().equals(Role.ROLE_ADMIN))
                ? currentUser.getUsername()
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
    public List<DocumentResponse> naturalLanguageSearch(String query, User currentUser) {
        log.info("[NLP Search Audit] 1. Request received for user {}: '{}'", currentUser != null ? currentUser.getUsername() : "anonymous", query);

        // Retrieve local candidates
        log.info("[NLP Search Audit] 2. Retrieving candidates locally from database...");
        List<DocumentAnalysis> candidates = getLocalCandidateAnalyses(query, currentUser);
        log.info("[NLP Search Audit] 2b. Retrieved {} candidate documents locally.", candidates.size());

        if (candidates.isEmpty()) {
            log.info("[NLP Search Audit] No candidates found in database. Returning empty list.");
            return List.of();
        }

        // Map candidates to concise DTOs for LLM to save tokens
        List<SearchCandidateDto> dtos = candidates.stream()
                .map(ca -> SearchCandidateDto.builder()
                        .id(ca.getDocument().getId().toString())
                        .name(ca.getDocument().getName())
                        .fileType(ca.getDocument().getFileType())
                        .summary(ca.getSummary())
                        .extractedData(ca.getExtractedData())
                        .build())
                .toList();

        try {
            String candidatesJson = objectMapper.writeValueAsString(dtos);
            
            // Execute Gemini query
            log.info("[NLP Search Audit] 3. Dispatching semantic ranking request to Gemini...");
            String aiResult = documentAiService.rankSearchResults(query, candidatesJson);
            log.info("[NLP Search Audit] 4. Gemini response received: {}", aiResult);

            // Clean Markdown code block wrapper if present
            if (aiResult != null) {
                aiResult = aiResult.trim();
                if (aiResult.startsWith("```json")) {
                    aiResult = aiResult.substring(7);
                }
                if (aiResult.endsWith("```")) {
                    aiResult = aiResult.substring(0, aiResult.length() - 3);
                }
                aiResult = aiResult.trim();
            }

            List<SearchResultDto> rankedResults = objectMapper.readValue(
                    aiResult,
                    new TypeReference<List<SearchResultDto>>() {}
            );

            log.info("[NLP Search Audit] 5. Parsed {} ranked search results.", rankedResults.size());

            // Build map of document ID -> result detail
            Map<UUID, SearchResultDto> rankedMap = rankedResults.stream()
                    .collect(Collectors.toMap(
                            r -> UUID.fromString(r.getId()),
                            r -> r,
                            (a, b) -> a
                    ));

            // Filter and sort original documents by their AI score
            List<DocumentResponse> finalResults = candidates.stream()
                    .filter(ca -> rankedMap.containsKey(ca.getDocument().getId()))
                    .map(ca -> {
                        SearchResultDto rankInfo = rankedMap.get(ca.getDocument().getId());
                        DocumentResponse response = mapToResponse(ca.getDocument());
                        log.info("[NLP Search Audit] Document '{}' matched with score {} (Reason: {})", 
                                 ca.getDocument().getName(), rankInfo.getRelevanceScore(), rankInfo.getReason());
                        return new RankedResponse(response, rankInfo.getRelevanceScore());
                    })
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(rr -> rr.response)
                    .toList();

            log.info("[NLP Search Audit] 6. Returning {} semantically matching documents.", finalResults.size());
            return finalResults;

        } catch (Exception e) {
            log.error("[NLP Search Audit] Error executing semantic search via AI, falling back to local text matches.", e);
            // Fallback: return all local candidates as responses
            return candidates.stream()
                    .map(ca -> mapToResponse(ca.getDocument()))
                    .toList();
        }
    }

    private List<DocumentAnalysis> getLocalCandidateAnalyses(String queryStr, User currentUser) {
        log.info("[NLP Search Audit] Querying DocumentAnalysis records from database...");
        List<DocumentAnalysis> allAnalyses = analysisRepository.findAll();
        if (currentUser != null && !currentUser.getRole().equals(Role.ROLE_ADMIN)) {
            allAnalyses = allAnalyses.stream()
                    .filter(a -> a.getDocument().getOwner().getId().equals(currentUser.getId()))
                    .toList();
        }
        log.info("[NLP Search Audit] User-filtered database query returned {} total records.", allAnalyses.size());

        if (allAnalyses.isEmpty()) {
            return List.of();
        }

        // Tokenize search query words
        String[] words = queryStr.toLowerCase().split("[^a-zA-Z0-9]+");
        List<String> queryWords = new java.util.ArrayList<>();
        for (String w : words) {
            if (w.length() > 2 && !isStopWord(w)) {
                queryWords.add(w);
            }
        }

        if (queryWords.isEmpty()) {
            log.info("[NLP Search Audit] No significant keywords found in query. Returning top 15 analyses.");
            return allAnalyses.stream().limit(15).toList();
        }

        log.info("[NLP Search Audit] Matching query words: {}", queryWords);

        // Rank by keyword match score
        List<RankedAnalysis> ranked = new java.util.ArrayList<>();
        for (DocumentAnalysis analysis : allAnalyses) {
            double score = 0.0;
            String name = analysis.getDocument().getName().toLowerCase();
            String summary = analysis.getSummary() != null ? analysis.getSummary().toLowerCase() : "";
            String extractedText = analysis.getExtractedText() != null ? analysis.getExtractedText().toLowerCase() : "";
            String extractedData = analysis.getExtractedData() != null ? analysis.getExtractedData().toLowerCase() : "";

            for (String qw : queryWords) {
                score += countOccurrences(name, qw) * 5.0; // Higher weight for filename match
                score += countOccurrences(summary, qw) * 2.0;
                score += countOccurrences(extractedData, qw) * 1.5;
                score += countOccurrences(extractedText, qw) * 1.0;
            }

            if (score > 0.0) {
                ranked.add(new RankedAnalysis(analysis, score));
            }
        }

        if (ranked.isEmpty()) {
            log.info("[NLP Search Audit] No keyword matches. Returning first 10 candidate documents as fallback.");
            return allAnalyses.stream().limit(10).toList();
        }

        ranked.sort((a, b) -> Double.compare(b.score, a.score));
        log.info("[NLP Search Audit] Found {} local keyword matches. Keeping top 15.", ranked.size());
        return ranked.stream()
                .map(r -> r.analysis)
                .limit(15)
                .toList();
    }

    private int countOccurrences(String source, String word) {
        if (source == null || source.isEmpty() || word == null || word.isEmpty()) return 0;
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }

    private boolean isStopWord(String word) {
        return List.of("the", "and", "for", "with", "this", "that", "from", "are", "was", "were", "been", "have", "has", "had", "what", "how", "why")
                .contains(word);
    }

    private static class RankedResponse {
        final DocumentResponse response;
        final double score;
        RankedResponse(DocumentResponse response, double score) {
            this.response = response;
            this.score = score;
        }
    }

    private static class RankedAnalysis {
        final DocumentAnalysis analysis;
        final double score;
        RankedAnalysis(DocumentAnalysis analysis, double score) {
            this.analysis = analysis;
            this.score = score;
        }
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchCandidateDto {
        private String id;
        private String name;
        private String fileType;
        private String summary;
        private String extractedData;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchResultDto {
        private String id;
        private Double relevanceScore;
        private String reason;
    }

    @Override
    public long countTotalDocuments(User currentUser) {
        if (currentUser != null && !currentUser.getRole().equals(Role.ROLE_ADMIN)) {
            return documentRepository.findByOwner(currentUser).size();
        }
        return documentRepository.count();
    }

    @Override
    public long countTodayUploads(User currentUser) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        if (currentUser != null && !currentUser.getRole().equals(Role.ROLE_ADMIN)) {
            return documentRepository.findByOwner(currentUser).stream()
                    .filter(d -> d.getCreatedAt().isAfter(startOfDay))
                    .count();
        }
        return documentRepository.countByCreatedAtAfter(startOfDay);
    }

    private DocumentResponse mapToResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .fileType(doc.getFileType())
                .status(doc.getStatus().name())
                .ownerUsername(doc.getOwner().getUsername())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
