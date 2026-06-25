package com.third.service;

import com.third.mapper.RagFileMapper;
import com.third.parser.factory.DocumentReaderFactory;
import com.third.pojo.entity.RagFile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagIngestionServiceTest {

    @Mock private DocumentReaderFactory documentReaderFactory;
    @Mock private VectorStore vectorStore;
    @Mock private RagFileMapper ragFileMapper;
    @Mock private QdrantManager qdrantManager;
    @Mock private FileService fileService;

    private SimpleMeterRegistry meterRegistry;
    private RagIngestionService service;
    private RagFile ragFile;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new RagIngestionService(documentReaderFactory, vectorStore, ragFileMapper,
                qdrantManager, fileService, meterRegistry);
        ragFile = new RagFile();
        ragFile.setId(17);
        ragFile.setKnowledgeBaseId(8);
        ragFile.setVersion(0.01);
        ragFile.setFileType("md");
        ragFile.setR2Key("knowledge_base/8/java.md");
        when(ragFileMapper.selectById(17)).thenReturn(ragFile);
        when(fileService.downloadObject(ragFile.getR2Key()))
                .thenReturn("Java Integer cache".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void marksDocumentReadyAfterVectorization() {
        assertTrue(service.processStoredFile(17));

        assertEquals("READY", ragFile.getStatus());
        verify(vectorStore).add(anyList());
        assertEquals(1.0, meterRegistry.get("blog.rag.ingestion.total")
                .tag("result", "success").counter().count());
    }

    @Test
    void marksDocumentFailedAndCompensatesVectorsWhenVectorizationFails() {
        doThrow(new IllegalStateException("Qdrant unavailable")).when(vectorStore).add(anyList());

        assertFalse(service.processStoredFile(17));

        assertEquals("FAILED", ragFile.getStatus());
        verify(qdrantManager).deleteByDocumentId(17);
        assertEquals(1.0, meterRegistry.get("blog.rag.ingestion.total")
                .tag("result", "failed").counter().count());
    }
}
