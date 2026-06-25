package com.third.service;

import com.third.config.RabbitMqConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@EnabledIfEnvironmentVariable(named = "RUN_RABBIT_INTEGRATION", matches = "true")
class RabbitMqBrokerIntegrationTest {

    private static CachingConnectionFactory connectionFactory;
    private static RabbitAdmin rabbitAdmin;
    private static RabbitTemplate rabbitTemplate;

    @BeforeAll
    static void connect() {
        connectionFactory = new CachingConnectionFactory("localhost", 5672);
        connectionFactory.setUsername("blog");
        connectionFactory.setPassword("change-me");
        rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitTemplate = new RabbitTemplate(connectionFactory);

        DirectExchange exchange = new DirectExchange(RabbitMqConfig.RAG_EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(RabbitMqConfig.RAG_DLX, true, false);
        Queue queue = QueueBuilder.durable(RabbitMqConfig.RAG_QUEUE)
                .deadLetterExchange(RabbitMqConfig.RAG_DLX)
                .deadLetterRoutingKey(RabbitMqConfig.RAG_DLQ_ROUTING_KEY)
                .build();
        Queue dlq = QueueBuilder.durable(RabbitMqConfig.RAG_DLQ).build();
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareExchange(dlx);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareQueue(dlq);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange)
                .with(RabbitMqConfig.RAG_ROUTING_KEY));
        rabbitAdmin.declareBinding(BindingBuilder.bind(dlq).to(dlx)
                .with(RabbitMqConfig.RAG_DLQ_ROUTING_KEY));
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @Test
    void declaresQueueAndRoundTripsMessageThroughRealBroker() {
        rabbitAdmin.purgeQueue(RabbitMqConfig.RAG_QUEUE, false);
        assertNotNull(rabbitAdmin.getQueueProperties(RabbitMqConfig.RAG_QUEUE));
        String payload = "integration-17";

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RAG_EXCHANGE, RabbitMqConfig.RAG_ROUTING_KEY, payload);

        assertEquals(payload, rabbitTemplate.receiveAndConvert(RabbitMqConfig.RAG_QUEUE, 3000));
    }

    @Test
    void comparesLocalExecutorAndPersistentRabbitRoundTripCost() throws Exception {
        int jobs = 200;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(jobs);
        long localStart = System.nanoTime();
        for (int i = 0; i < jobs; i++) {
            executor.execute(latch::countDown);
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long localMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - localStart);
        executor.shutdownNow();

        rabbitAdmin.purgeQueue(RabbitMqConfig.RAG_QUEUE, false);
        long rabbitStart = System.nanoTime();
        for (int i = 0; i < jobs; i++) {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.RAG_EXCHANGE,
                    RabbitMqConfig.RAG_ROUTING_KEY,
                    "benchmark-" + i,
                    message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    });
        }
        for (int i = 0; i < jobs; i++) {
            assertNotNull(rabbitTemplate.receiveAndConvert(RabbitMqConfig.RAG_QUEUE, 3000));
        }
        long rabbitMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - rabbitStart);

        System.out.printf("RAG_TRANSPORT_BENCHMARK jobs=%d local_ms=%d rabbitmq_ms=%d%n",
                jobs, localMs, rabbitMs);
    }
}
