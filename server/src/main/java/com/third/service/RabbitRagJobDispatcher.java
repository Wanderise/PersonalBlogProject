package com.third.service;

import com.third.config.RabbitMqConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rag.transport", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitRagJobDispatcher implements RagJobDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    public RabbitRagJobDispatcher(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void dispatch(Integer ragFileId) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RAG_EXCHANGE,
                RabbitMqConfig.RAG_ROUTING_KEY,
                String.valueOf(ragFileId),
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("x-rag-file-id", ragFileId);
                    return message;
                });
        meterRegistry.counter("blog.rag.dispatch.total", "transport", "rabbitmq").increment();
    }
}
