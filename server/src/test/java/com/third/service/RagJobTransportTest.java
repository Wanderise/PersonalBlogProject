package com.third.service;

import com.third.config.RabbitMqConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class RagJobTransportTest {

    @Test
    void localTransportSubmitsWorkToExecutor() {
        Executor executor = mock(Executor.class);
        RagIngestionService ingestionService = mock(RagIngestionService.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LocalRagJobDispatcher dispatcher = new LocalRagJobDispatcher(executor, ingestionService, registry);

        dispatcher.dispatch(17);

        verify(executor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
        assertEquals(1.0, registry.get("blog.rag.dispatch.total")
                .tag("transport", "local").counter().count());
    }

    @Test
    void rabbitTransportPublishesPersistentMessage() throws Exception {
        RabbitTemplate template = mock(RabbitTemplate.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RabbitRagJobDispatcher dispatcher = new RabbitRagJobDispatcher(template, registry);
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        dispatcher.dispatch(17);

        verify(template).convertAndSend(eq(RabbitMqConfig.RAG_EXCHANGE),
                eq(RabbitMqConfig.RAG_ROUTING_KEY), eq("17"), processor.capture());
        Message message = processor.getValue().postProcessMessage(new Message(new byte[0]));
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
        assertEquals(Integer.valueOf(17),
                (Integer) message.getMessageProperties().getHeader("x-rag-file-id"));
        assertEquals(1.0, registry.get("blog.rag.dispatch.total")
                .tag("transport", "rabbitmq").counter().count());
    }

    @Test
    void listenerRejectsFailedIngestionSoBrokerCanRetryAndDeadLetter() {
        RagIngestionService ingestionService = mock(RagIngestionService.class);
        when(ingestionService.processStoredFile(17)).thenReturn(false);
        RagIngestionListener listener = new RagIngestionListener(ingestionService);

        assertThrows(IllegalStateException.class, () -> listener.consume("17"));
    }
}
