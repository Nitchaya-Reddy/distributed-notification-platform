package com.nitchaya.notify.worker.service;

import com.nitchaya.notify.common.NotificationStatus;
import com.nitchaya.notify.common.NotificationStatusEvent;
import com.nitchaya.notify.worker.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StatusPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StatusPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(UUID id, NotificationStatus status, String message) {
        try {
            kafkaTemplate.send(KafkaConfig.STATUS, id.toString(),
                    new NotificationStatusEvent(id, status, message, Instant.now())).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not publish status event", e);
        }
    }
}
