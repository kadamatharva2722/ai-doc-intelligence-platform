package com.enterprise.docintel.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;

public class DocumentAiServiceTest {

    private ChatClient chatClient;
    private DocumentAiService documentAiService;

    @BeforeEach
    public void setup() {
        // Use Mockito's RETURNS_DEEP_STUBS to mock the fluent API chain easily
        chatClient = Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        documentAiService = new DocumentAiService(chatClient);

        // Inject configuration parameters using ReflectionTestUtils
        ReflectionTestUtils.setField(documentAiService, "chunkSize", 100);
        ReflectionTestUtils.setField(documentAiService, "chunkOverlap", 10);
        ReflectionTestUtils.setField(documentAiService, "maxSingleRequestTokens", 150);
        ReflectionTestUtils.setField(documentAiService, "requestDelayMs", 0); // No sleep in tests
        ReflectionTestUtils.setField(documentAiService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(documentAiService, "backoffMultiplier", 2.0);
    }

    @Test
    public void testCountTokens() {
        String text = "Hello world! This is a simple test document containing some words to test JTokkit token estimation.";
        int count = documentAiService.countTokens(text);
        Assertions.assertTrue(count > 0);
        Assertions.assertEquals(19, count); // JTokkit token count for CL100K_BASE
    }

    @Test
    public void testGenerateSummary_SmallText() {
        String text = "Small text that fits in one request.";
        Mockito.when(chatClient.prompt(any(Prompt.class)).call().content())
               .thenReturn("Direct summary response.");

        String summary = documentAiService.generateSummary(text);

        Assertions.assertEquals("Direct summary response.", summary);
        Mockito.verify(chatClient, Mockito.times(1)).prompt(any(Prompt.class));
    }

    @Test
    public void testGenerateSummary_LargeText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append("This is a sentence containing important details about enterprise document intelligence systems. ");
        }
        String largeText = sb.toString();
        
        Mockito.when(chatClient.prompt(any(Prompt.class)).call().content())
               .thenReturn("Chunk summary.") // Chunk 1
               .thenReturn("Chunk summary.") // Chunk 2
               .thenReturn("Chunk summary.") // Chunk 3
               .thenReturn("Chunk summary.") // Chunk 4
               .thenReturn("Final consolidated summary."); // Final single summary

        String summary = documentAiService.generateSummary(largeText);

        Assertions.assertEquals("Final consolidated summary.", summary);
        Mockito.verify(chatClient, Mockito.atLeast(3)).prompt(any(Prompt.class));
    }

    @Test
    public void testAskDocumentQuestion_RAGSelection() {
        String docText = "Section One: This talks about financial revenues and profit margins. " +
                         "Section Two: This explains the engineering architecture and microservices design. " +
                         "Section Three: This describes customer support policies and SLA timings.";

        Mockito.when(chatClient.prompt(any(Prompt.class)).call().content())
               .thenReturn("The SLA timing is 24 hours.");

        String answer = documentAiService.askDocumentQuestion(docText, "What are the SLA timings for support?");

        Assertions.assertEquals("The SLA timing is 24 hours.", answer);
        Mockito.verify(chatClient, Mockito.times(1)).prompt(any(Prompt.class));
    }
}
