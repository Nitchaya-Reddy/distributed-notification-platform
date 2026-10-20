package com.nitchaya.notify.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitchaya.notify.api.repository.OutboxRepository;
import com.nitchaya.notify.common.NotificationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisher {
    private final OutboxRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public OutboxPublisher(OutboxRepository repository,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           ObjectMapper objectMapper,
                           @Value("${app.outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:500}")
    @Transactional
    public void publishBatch() {
        var batch = repository.lockNextBatch(batchSize);
        for (var row : batch) {
            try {
                NotificationRequestedEvent event = objectMapper.readValue(row.getPayload(), NotificationRequestedEvent.class);
                kafkaTemplate.send(row.getTopic(), row.getAggregateId().toString(), event)
                        .get(5, TimeUnit.SECONDS);
                row.markPublished(Instant.now());
            } catch (Exception e) {
                throw new IllegalStateException("Kafka publish failed; outbox row remains pending", e);
            }
        }
    }
}
