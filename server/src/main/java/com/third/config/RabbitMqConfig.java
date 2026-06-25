package com.third.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.rag.transport", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMqConfig {

    public static final String RAG_EXCHANGE = "blog.rag.exchange";
    public static final String RAG_QUEUE = "blog.rag.ingestion";
    public static final String RAG_ROUTING_KEY = "rag.ingest";
    public static final String RAG_DLX = "blog.rag.dlx";
    public static final String RAG_DLQ = "blog.rag.ingestion.dlq";
    public static final String RAG_DLQ_ROUTING_KEY = "rag.ingest.failed";

    @Bean
    DirectExchange ragExchange() {
        return new DirectExchange(RAG_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange ragDeadLetterExchange() {
        return new DirectExchange(RAG_DLX, true, false);
    }

    @Bean
    Queue ragQueue() {
        return QueueBuilder.durable(RAG_QUEUE)
                .deadLetterExchange(RAG_DLX)
                .deadLetterRoutingKey(RAG_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue ragDeadLetterQueue() {
        return QueueBuilder.durable(RAG_DLQ).build();
    }

    @Bean
    Binding ragBinding(Queue ragQueue, DirectExchange ragExchange) {
        return BindingBuilder.bind(ragQueue).to(ragExchange).with(RAG_ROUTING_KEY);
    }

    @Bean
    Binding ragDeadLetterBinding(Queue ragDeadLetterQueue, DirectExchange ragDeadLetterExchange) {
        return BindingBuilder.bind(ragDeadLetterQueue)
                .to(ragDeadLetterExchange)
                .with(RAG_DLQ_ROUTING_KEY);
    }
}
