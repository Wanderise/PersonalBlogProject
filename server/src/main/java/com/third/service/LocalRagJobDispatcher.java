package com.third.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@ConditionalOnProperty(name = "app.rag.transport", havingValue = "local")
@Slf4j
public class LocalRagJobDispatcher implements RagJobDispatcher {

    private final Executor executor;
    private final RagIngestionService ingestionService;
    private final MeterRegistry meterRegistry;

    public LocalRagJobDispatcher(@Qualifier("ragTaskExecutor") Executor executor,
                                 RagIngestionService ingestionService,
                                 MeterRegistry meterRegistry) {
        this.executor = executor;
        this.ingestionService = ingestionService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void dispatch(Integer ragFileId) {
        executor.execute(() -> {
            if (!ingestionService.processStoredFile(ragFileId)) {
                log.warn("Local RAG job failed: ragFileId={}", ragFileId);
            }
        });
        meterRegistry.counter("blog.rag.dispatch.total", "transport", "local").increment();
    }
}
