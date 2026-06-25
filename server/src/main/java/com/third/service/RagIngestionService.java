package com.third.service;

import com.third.mapper.RagFileMapper;
import com.third.parser.factory.DocumentReaderFactory;
import com.third.pojo.entity.RagFile;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class RagIngestionService {

    private final DocumentReaderFactory documentReaderFactory;
    private final VectorStore vectorStore;
    private final RagFileMapper ragFileMapper;
    private final QdrantManager qdrantManager;
    private final FileService fileService;
    private final MeterRegistry meterRegistry;
    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    public RagIngestionService(DocumentReaderFactory documentReaderFactory,
                               VectorStore vectorStore,
                               RagFileMapper ragFileMapper,
                               QdrantManager qdrantManager,
                               FileService fileService,
                               MeterRegistry meterRegistry) {
        this.documentReaderFactory = documentReaderFactory;
        this.vectorStore = vectorStore;
        this.ragFileMapper = ragFileMapper;
        this.qdrantManager = qdrantManager;
        this.fileService = fileService;
        this.meterRegistry = meterRegistry;
    }

    public boolean processStoredFile(Integer ragFileId) {
        RagFile ragFile = ragFileMapper.selectById(ragFileId);
        if (ragFile == null) {
            return true;
        }
        return process(ragFileId, () -> {
            byte[] content = fileService.downloadObject(ragFile.getR2Key());
            if ("md".equalsIgnoreCase(ragFile.getFileType())) {
                return List.of(new Document(new String(content, StandardCharsets.UTF_8)));
            }
            return documentReaderFactory.getReader(ragFile.getFileType())
                    .read(new ByteArrayResource(content), ragFile.getFileType());
        });
    }

    private boolean process(Integer ragFileId, Supplier<List<Document>> reader) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            RagFile ragFile = ragFileMapper.selectById(ragFileId);
            if (ragFile == null) {
                return true;
            }
            updateStatus(ragFile, "PROCESSING");
            List<Document> documents = textSplitter.apply(reader.get());
            for (Document document : documents) {
                document.getMetadata().put("document_id", ragFile.getId());
                document.getMetadata().put("kb_id", ragFile.getKnowledgeBaseId());
                document.getMetadata().put("version", ragFile.getVersion());
            }
            batchAdd(documents);

            RagFile current = ragFileMapper.selectById(ragFileId);
            if (current == null) {
                qdrantManager.deleteByDocumentId(ragFileId);
                return true;
            }
            updateStatus(current, "READY");
            counter("success").increment();
            return true;
        } catch (Exception e) {
            log.error("RAG document ingestion failed: ragFileId={}", ragFileId, e);
            try {
                qdrantManager.deleteByDocumentId(ragFileId);
            } catch (Exception cleanupError) {
                log.error("RAG vector compensation failed: ragFileId={}", ragFileId, cleanupError);
            }
            RagFile failed = ragFileMapper.selectById(ragFileId);
            if (failed != null) {
                updateStatus(failed, "FAILED");
            }
            counter("failed").increment();
            return false;
        } finally {
            sample.stop(Timer.builder("blog.rag.ingestion.duration")
                    .description("RAG document ingestion duration")
                    .register(meterRegistry));
        }
    }

    private void batchAdd(List<Document> documents) {
        for (int i = 0; i < documents.size(); i += 10) {
            int end = Math.min(i + 10, documents.size());
            vectorStore.add(new ArrayList<>(documents.subList(i, end)));
        }
    }

    private void updateStatus(RagFile ragFile, String status) {
        ragFile.setStatus(status);
        ragFileMapper.updateById(ragFile);
    }

    private Counter counter(String result) {
        return Counter.builder("blog.rag.ingestion.total")
                .tag("result", result)
                .register(meterRegistry);
    }
}
