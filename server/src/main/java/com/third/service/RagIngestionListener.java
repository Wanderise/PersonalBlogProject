package com.third.service;

import com.third.config.RabbitMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rag.transport", havingValue = "rabbitmq", matchIfMissing = true)
@Slf4j
public class RagIngestionListener {

    private final RagIngestionService ingestionService;

    public RagIngestionListener(RagIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @RabbitListener(queues = RabbitMqConfig.RAG_QUEUE,
            concurrency = "${app.rag.rabbitmq.concurrency:2}")
    public void consume(String ragFileId) {
        Integer id;
        try {
            id = Integer.valueOf(ragFileId);
        } catch (NumberFormatException e) {
            log.error("Discarding malformed RAG job: {}", ragFileId);
            throw e;
        }
        if (!ingestionService.processStoredFile(id)) {
            throw new IllegalStateException("RAG ingestion failed: ragFileId=" + id);
        }
    }
}
