package com.third.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvaluationDatasetTest {

    @Test
    void evaluationCasesHaveRequiredFields() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/rag-evaluation/cases.json")) {
            assertNotNull(input);
            JsonNode cases = new ObjectMapper().readTree(input);
            assertTrue(cases.isArray());
            assertFalse(cases.isEmpty());
            for (JsonNode item : cases) {
                assertTrue(item.hasNonNull("knowledgeBase"));
                assertTrue(item.hasNonNull("question"));
                assertTrue(item.has("expectedTerms"));
                assertTrue(item.has("answerable"));
            }
        }
    }
}
