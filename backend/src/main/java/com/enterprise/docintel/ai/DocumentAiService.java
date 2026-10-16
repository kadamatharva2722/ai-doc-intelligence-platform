package com.enterprise.docintel.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAiService {

    private final ChatClient chatClient;

    @Value("${app.ai.chunk-size:2000}")
    private int chunkSize;

    @Value("${app.ai.chunk-overlap:200}")
    private int chunkOverlap;

    @Value("${app.ai.max-single-request-tokens:3000}")
    private int maxSingleRequestTokens;

    @Value("${app.ai.request-delay-ms:3000}")
    private int requestDelayMs;

    @Value("${app.ai.max-retry-attempts:5}")
    private int maxRetryAttempts;

    @Value("${app.ai.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    // Fast, thread-safe, local tokenizer for token counting using standard CL100K (GPT-4/Llama compatible)
    private final Encoding encoding = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    @FunctionalInterface
    public interface LlmCall<T> {
        T execute();
    }

    /**
     * Estimates the token count of a given string.
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return encoding.countTokens(text);
    }

    /**
     * Executes an LLM call with rate-limit retries and a cooldown delay.
     */
    private <T> T callWithRetry(LlmCall<T> call, String actionDescription) {
        int attempt = 1;
        long delay = 1000; // Base delay: 1 second

        while (true) {
            try {
                T result = call.execute();
                cooldown(); // Proactive throttling delay
                return result;
            } catch (Exception e) {
                boolean isRateLimit = isRateLimitException(e);
                if (attempt < maxRetryAttempts) {
                    long sleepTime = isRateLimit ? delay * 3 : (long) (delay * Math.pow(backoffMultiplier, attempt - 1));
                    log.warn("Temporary error during {}: {}. Retrying in {} ms (attempt {}/{})", 
                             actionDescription, e.getMessage(), sleepTime, attempt, maxRetryAttempts);
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry wait interrupted", ie);
                    }
                    attempt++;
                    delay = (long) (delay * backoffMultiplier);
                } else {
                    log.error("Failed executing {} after {} attempts. Error: {}", actionDescription, maxRetryAttempts, e.getMessage(), e);
                    throw e;
                }
            }
        }
    }

    private boolean isRateLimitException(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg != null && (msg.contains("429") || msg.toLowerCase().contains("too many requests") || msg.toLowerCase().contains("rate limit"))) {
            return true;
        }
        return isRateLimitException(t.getCause());
    }

    private void cooldown() {
        if (requestDelayMs > 0) {
            try {
                Thread.sleep(requestDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Cooldown delay interrupted", e);
            }
        }
    }

    private String callLlm(Prompt prompt, String actionName) {
        return callWithRetry(() -> chatClient.prompt(prompt).call().content(), actionName);
    }

    private List<String> splitTextIntoChunks(String text) {
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 10, 10000, true);
        List<org.springframework.ai.document.Document> inputDocs = List.of(new org.springframework.ai.document.Document(text));
        List<org.springframework.ai.document.Document> splitDocs = splitter.apply(inputDocs);
        return splitDocs.stream()
                .map(org.springframework.ai.document.Document::getText)
                .toList();
    }

    /**
     * Recursively generates a MapReduce summary of a document.
     */
    public String generateSummary(String textContent) {
        log.info("Generating AI Summary. Length: {} chars", textContent == null ? 0 : textContent.length());
        if (textContent == null || textContent.trim().isEmpty()) {
            return "No text content available to summarize.";
        }

        int tokenCount = countTokens(textContent);
        log.info("Estimated token count: {}", tokenCount);

        // If the content fits in a single request, summarize directly
        if (tokenCount <= maxSingleRequestTokens) {
            return generateSingleSummary(textContent);
        }

        // Otherwise, split and summarize chunks
        log.info("Text is too large ({} tokens). Splitting into chunks of size {}.", tokenCount, chunkSize);
        List<String> chunks = splitTextIntoChunks(textContent);
        log.info("Split into {} chunks.", chunks.size());

        List<String> chunkSummaries = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            log.info("Summarizing chunk {}/{} ({} tokens)", i + 1, chunks.size(), countTokens(chunk));
            
            PromptTemplate template = new PromptTemplate("""
                    Generate a brief, informative summary of the following section of a document.
                    Focus on key facts, numbers, and core details.
                    
                    Section Content:
                    {chunk}
                    """);
            Prompt prompt = template.create(Map.of("chunk", chunk));
            String summary = callLlm(prompt, "Summarizing chunk " + (i + 1));
            chunkSummaries.add(summary);
        }

        // Merge summaries
        String mergedSummaries = String.join("\n\n", chunkSummaries);
        
        // Recursively summarize the merged summaries
        return generateSummary(mergedSummaries);
    }

    private String generateSingleSummary(String textContent) {
        log.info("Generating final summary directly ({} tokens)", countTokens(textContent));
        PromptTemplate template = new PromptTemplate("""
                Analyze the following document/summaries and generate
                a professional business summary in 3-5 sentences.
                
                Document Content:
                {document}
                """);
        Prompt prompt = template.create(Map.of("document", textContent));
        return callLlm(prompt, "Generating final summary");
    }

    /**
     * Extracts structured JSON metadata from a large document in a token-safe manner.
     */
    public String extractStructuredData(String textContent) {
        log.info("Extracting structured data. Length: {} chars", textContent == null ? 0 : textContent.length());
        if (textContent == null || textContent.trim().isEmpty()) {
            return "{}";
        }

        int tokenCount = countTokens(textContent);
        if (tokenCount <= maxSingleRequestTokens) {
            return extractStructuredDataDirect(textContent);
        }

        log.info("Text is too large for structured data extraction ({} tokens). Extracting from chunks.", tokenCount);
        List<String> chunks = splitTextIntoChunks(textContent);
        List<String> chunkJsons = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            log.info("Extracting structured data from chunk {}/{}", i + 1, chunks.size());
            
            PromptTemplate template = new PromptTemplate("""
                    Extract important structured metadata (e.g., keys, names, dates, values, amounts) from the following document section.
                    Return ONLY valid JSON.
                    
                    Section Content:
                    {chunk}
                    """);
            Prompt prompt = template.create(Map.of("chunk", chunk));
            String jsonResult = callLlm(prompt, "Extracting metadata from chunk " + (i + 1));
            chunkJsons.add(jsonResult);
        }

        // Merge and consolidate JSONs
        log.info("Consolidating {} JSON extractions into a final combined JSON", chunkJsons.size());
        return consolidateJsons(chunkJsons);
    }

    private String extractStructuredDataDirect(String textContent) {
        PromptTemplate template = new PromptTemplate("""
                Extract key structured attributes (e.g. Document Type, Title, Issue Date, Due Date, Total Amount, Tax/Fees, Customer/Vendor Name, Key Terms, Reference Numbers) from the document below.
                Return ONLY a valid JSON object mapping attributes to their extracted values. Do NOT wrap in markdown or backticks.
                
                Document:
                {document}
                """);
        Prompt prompt = template.create(Map.of("document", textContent));
        String rawResult = callLlm(prompt, "Direct structured data extraction");
        return cleanJsonString(rawResult);
    }

    private String cleanJsonString(String raw) {
        if (raw == null) return "{}";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String consolidateJsons(List<String> jsons) {
        if (jsons.isEmpty()) {
            return "{}";
        }
        if (jsons.size() == 1) {
            return jsons.get(0);
        }
        
        String combined = String.join("\n\n---\n\n", jsons);
        if (countTokens(combined) <= maxSingleRequestTokens) {
            PromptTemplate template = new PromptTemplate("""
                    Merge and consolidate the following JSON metadata chunks extracted from a document into a single, clean, valid JSON object.
                    Deduplicate information, reconcile discrepancies, and structure it professionally.
                    Return ONLY valid JSON.
                    
                    JSON Chunks:
                    {jsons}
                    """);
            Prompt prompt = template.create(Map.of("jsons", combined));
            return callLlm(prompt, "Consolidating JSON metadata");
        }
        
        log.info("JSON size exceeds limit. Partitioning into smaller groups to merge.");
        List<String> groupedJsons = new ArrayList<>();
        List<String> currentGroup = new ArrayList<>();
        int currentTokens = 0;
        
        for (String json : jsons) {
            int tokens = countTokens(json);
            if (currentTokens + tokens > maxSingleRequestTokens && !currentGroup.isEmpty()) {
                groupedJsons.add(consolidateJsons(currentGroup));
                currentGroup = new ArrayList<>();
                currentTokens = 0;
            }
            currentGroup.add(json);
            currentTokens += tokens;
        }
        if (!currentGroup.isEmpty()) {
            groupedJsons.add(consolidateJsons(currentGroup));
        }
        
        return consolidateJsons(groupedJsons);
    }

    /**
     * Analyzes a document for risks and issues in a token-safe manner.
     */
    public String identifyIssuesAndConfidence(String textContent) {
        log.info("Identifying issues and confidence. Length: {} chars", textContent == null ? 0 : textContent.length());
        if (textContent == null || textContent.trim().isEmpty()) {
            return "No issues identified.";
        }

        int tokenCount = countTokens(textContent);
        if (tokenCount <= maxSingleRequestTokens) {
            return identifyIssuesDirect(textContent);
        }

        log.info("Text is too large ({} tokens). Analyzing chunks for issues.", tokenCount);
        List<String> chunks = splitTextIntoChunks(textContent);
        List<String> chunkReports = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            log.info("Analyzing chunk {}/{} for issues", i + 1, chunks.size());
            PromptTemplate template = new PromptTemplate("""
                    Analyze the following document section for:
                    - Missing Information
                    - Risks
                    - Inconsistencies
                    Also provide a local confidence score for this section.
                    
                    Section Content:
                    {chunk}
                    """);
            Prompt prompt = template.create(Map.of("chunk", chunk));
            String report = callLlm(prompt, "Analyzing issues in chunk " + (i + 1));
            chunkReports.add(report);
        }

        // Merge reports
        return synthesizeIssueReports(chunkReports);
    }

    private String identifyIssuesDirect(String textContent) {
        PromptTemplate template = new PromptTemplate("""
                Find
                - Missing Information
                - Risks
                - Inconsistencies
                Also provide confidence score.
                
                Document:
                {document}
                """);
        Prompt prompt = template.create(Map.of("document", textContent));
        return callLlm(prompt, "Direct issues analysis");
    }

    private String synthesizeIssueReports(List<String> reports) {
        if (reports.isEmpty()) {
            return "No issues identified.";
        }
        if (reports.size() == 1) {
            return reports.get(0);
        }

        String combined = String.join("\n\n---\n\n", reports);
        if (countTokens(combined) <= maxSingleRequestTokens) {
            PromptTemplate template = new PromptTemplate("""
                    Synthesize the following list of risks, missing information, and inconsistencies found in different sections of a document.
                    Deduplicate issues, group them logically, and provide a single consolidated issues report.
                    Include an overall aggregated confidence score (e.g. 'Confidence Score: 0.95') based on the findings.
                    
                    Reports:
                    {reports}
                    """);
            Prompt prompt = template.create(Map.of("reports", combined));
            return callLlm(prompt, "Synthesizing issues report");
        }

        log.info("Issues reports exceed limit. Partitioning to merge.");
        List<String> groupedReports = new ArrayList<>();
        List<String> currentGroup = new ArrayList<>();
        int currentTokens = 0;
        
        for (String report : reports) {
            int tokens = countTokens(report);
            if (currentTokens + tokens > maxSingleRequestTokens && !currentGroup.isEmpty()) {
                groupedReports.add(synthesizeIssueReports(currentGroup));
                currentGroup = new ArrayList<>();
                currentTokens = 0;
            }
            currentGroup.add(report);
            currentTokens += tokens;
        }
        if (!currentGroup.isEmpty()) {
            groupedReports.add(synthesizeIssueReports(currentGroup));
        }

        return synthesizeIssueReports(groupedReports);
    }

    /**
     * Answers a question relative to a large document using an in-memory RAG keyword-ranking approach.
     */
    public String askDocumentQuestion(String text, String question) {
        log.info("Answering question. Text length: {} chars, Question: {}", text == null ? 0 : text.length(), question);
        if (text == null || text.trim().isEmpty()) {
            return "The document contains no text.";
        }

        int tokenCount = countTokens(text);
        if (tokenCount <= maxSingleRequestTokens) {
            return askDocumentQuestionDirect(text, question);
        }

        log.info("Document is too large ({} tokens). Ranking chunks to find relevant context.", tokenCount);
        List<String> chunks = splitTextIntoChunks(text);
        
        // Rank chunks by keyword overlap with the question
        List<RankedChunk> rankedChunks = new ArrayList<>();
        List<String> questionWords = tokenizeAndNormalize(question);

        for (String chunk : chunks) {
            double score = computeRelevanceScore(chunk, questionWords);
            rankedChunks.add(new RankedChunk(chunk, score));
        }

        // Sort by relevance score in descending order
        rankedChunks.sort((a, b) -> Double.compare(b.score, a.score));

        // Select top chunks that fit in the single request limit
        List<String> selectedChunks = new ArrayList<>();
        int accumulatedTokens = 0;
        
        for (RankedChunk rc : rankedChunks) {
            int chunkTokens = countTokens(rc.text);
            // Reserve 500 tokens for system prompt and instructions
            if (accumulatedTokens + chunkTokens < (maxSingleRequestTokens - 500)) {
                selectedChunks.add(rc.text);
                accumulatedTokens += chunkTokens;
            } else if (selectedChunks.isEmpty()) {
                selectedChunks.add(rc.text);
                break;
            } else {
                break;
            }
        }

        log.info("Selected {} of {} chunks as relevant context ({} tokens).", selectedChunks.size(), chunks.size(), accumulatedTokens);
        String combinedContext = String.join("\n\n... [Section Break] ...\n\n", selectedChunks);

        PromptTemplate template = new PromptTemplate("""
                Answer the question based ONLY on the provided document sections.
                If the answer cannot be found in the provided sections, state that you do not have enough information to answer.
                
                Document Sections:
                {document}
                
                Question:
                {question}
                """);
        Prompt prompt = template.create(Map.of(
                "document", combinedContext,
                "question", question
        ));
        return callLlm(prompt, "Answering question");
    }

    private String askDocumentQuestionDirect(String text, String question) {
        PromptTemplate template = new PromptTemplate("""
                Answer ONLY using the document.
                
                Document:
                {document}
                
                Question:
                {question}
                """);
        Prompt prompt = template.create(Map.of(
                "document", text,
                "question", question
        ));
        return callLlm(prompt, "Answering question directly");
    }

    /**
     * Compares two large documents by generating comprehensive structured outlines and comparing them.
     */
    public String compareDocuments(String first, String second) {
        log.info("Comparing two documents. Lengths: first={}, second={}", 
                 first == null ? 0 : first.length(), second == null ? 0 : second.length());
        
        int firstTokens = countTokens(first);
        int secondTokens = countTokens(second);

        // If both combined fit within the single request limit, compare directly
        if (firstTokens + secondTokens <= maxSingleRequestTokens) {
            return compareDirectly(first, second);
        }

        log.info("One or both documents are too large (doc1={} tokens, doc2={} tokens). Generating outlines first.", 
                 firstTokens, secondTokens);
        
        String firstOutline = generateOutline(first);
        String secondOutline = generateOutline(second);

        log.info("Comparing outlines. Outline tokens: doc1={}, doc2={}", 
                 countTokens(firstOutline), countTokens(secondOutline));

        PromptTemplate template = new PromptTemplate("""
                Compare these two documents based on their structured outlines.
                Focus on highlighting key differences (Added, Removed, Changed).
                
                Document 1 Outline:
                {doc1}
                
                Document 2 Outline:
                {doc2}
                """);
        Prompt prompt = template.create(Map.of(
                "doc1", firstOutline,
                "doc2", secondOutline
        ));
        return callLlm(prompt, "Comparing document outlines");
    }

    /**
     * Filters and ranks search results using Gemini semantic understanding.
     */
    public String rankSearchResults(String query, String candidatesJson) {
        log.info("Prompting AI model to rank search candidates for query: '{}'", query);
        PromptTemplate template = new PromptTemplate("""
                You are an enterprise document search relevance system.
                Analyze the user's search query and evaluate the relevance of each document in the provided list of candidates.
                
                For each candidate document, determine if it is relevant to the search query. 
                - Relevance is based on semantic matches with the document name, its summary, and extracted metadata.
                - Assign a relevance score between 0.0 (completely irrelevant) and 1.0 (perfect match).
                - Completely filter out documents that have a relevance score less than 0.2.
                
                You MUST return ONLY a valid JSON array of objects in this exact format (no markdown formatting, no comments, no extra text):
                [
                  {
                    "id": "document-uuid-string",
                    "relevanceScore": 0.85,
                    "reason": "Brief explanation of why this document matches"
                  }
                ]
                
                Search Query: {query}
                
                Candidate Documents (JSON):
                {candidatesJson}
                """);
        Prompt prompt = template.create(Map.of(
                "query", query,
                "candidatesJson", candidatesJson
        ));
        
        String response = callLlm(prompt, "Semantic search ranking");
        log.info("AI ranking response received: {}", response);
        return response;
    }

    private String compareDirectly(String first, String second) {
        PromptTemplate template = new PromptTemplate("""
                Compare these two documents.
                Mention:
                - Added
                - Removed
                - Changed
                
                Document1:
                {doc1}
                
                Document2:
                {doc2}
                """);
        Prompt prompt = template.create(Map.of(
                "doc1", first,
                "doc2", second
        ));
        return callLlm(prompt, "Direct comparison");
    }

    private String generateOutline(String textContent) {
        int tokenCount = countTokens(textContent);
        if (tokenCount <= maxSingleRequestTokens) {
            PromptTemplate template = new PromptTemplate("""
                    Generate a comprehensive, structured outline of the following document.
                    Include major sections, key terms, dates, and amounts.
                    
                    Document:
                    {document}
                    """);
            Prompt prompt = template.create(Map.of("document", textContent));
            return callLlm(prompt, "Direct outline generation");
        }

        List<String> chunks = splitTextIntoChunks(textContent);
        List<String> chunkOutlines = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            PromptTemplate template = new PromptTemplate("""
                    Generate a structured outline for this section of a document.
                    Include key headers, bullet points, and main figures.
                    
                    Section Content:
                    {chunk}
                    """);
            Prompt prompt = template.create(Map.of("chunk", chunk));
            String outline = callLlm(prompt, "Generating outline for chunk " + (i + 1));
            chunkOutlines.add(outline);
        }

        return synthesizeOutlines(chunkOutlines);
    }

    private String synthesizeOutlines(List<String> outlines) {
        if (outlines.isEmpty()) {
            return "Empty document.";
        }
        if (outlines.size() == 1) {
            return outlines.get(0);
        }

        String combined = String.join("\n\n---\n\n", outlines);
        if (countTokens(combined) <= maxSingleRequestTokens) {
            PromptTemplate template = new PromptTemplate("""
                    Consolidate the following outlines from different sections of a document into a single comprehensive, coherent, hierarchical document outline.
                    
                    Outlines:
                    {outlines}
                    """);
            Prompt prompt = template.create(Map.of("outlines", combined));
            return callLlm(prompt, "Synthesizing outlines");
        }

        List<String> groupedOutlines = new ArrayList<>();
        List<String> currentGroup = new ArrayList<>();
        int currentTokens = 0;
        
        for (String outline : outlines) {
            int tokens = countTokens(outline);
            if (currentTokens + tokens > maxSingleRequestTokens && !currentGroup.isEmpty()) {
                groupedOutlines.add(synthesizeOutlines(currentGroup));
                currentGroup = new ArrayList<>();
                currentTokens = 0;
            }
            currentGroup.add(outline);
            currentTokens += tokens;
        }
        if (!currentGroup.isEmpty()) {
            groupedOutlines.add(synthesizeOutlines(currentGroup));
        }

        return synthesizeOutlines(groupedOutlines);
    }

    // Helper classes and methods for RAG-lite Context Selection
    private static class RankedChunk {
        final String text;
        final double score;

        RankedChunk(String text, double score) {
            this.text = text;
            this.score = score;
        }
    }

    private List<String> tokenizeAndNormalize(String text) {
        if (text == null) return List.of();
        String[] words = text.toLowerCase().split("[^a-zA-Z0-9]+");
        List<String> filtered = new ArrayList<>();
        for (String w : words) {
            if (w.length() > 2 && !isStopWord(w)) {
                filtered.add(w);
            }
        }
        return filtered;
    }

    private boolean isStopWord(String word) {
        return List.of("the", "and", "for", "with", "this", "that", "from", "are", "was", "were", "been", "have", "has", "had", "what", "how", "why")
                .contains(word);
    }

    private double computeRelevanceScore(String chunk, List<String> queryWords) {
        if (chunk == null || queryWords.isEmpty()) return 0.0;
        String chunkLower = chunk.toLowerCase();
        double score = 0.0;
        for (String word : queryWords) {
            int index = 0;
            int count = 0;
            while ((index = chunkLower.indexOf(word, index)) != -1) {
                count++;
                index += word.length();
            }
            if (count > 0) {
                score += (1 + Math.log(count)); // Term frequency scaling
            }
        }
        return score;
    }
}