package com.nitchaya.notify.worker.service;

import com.nitchaya.notify.common.DeliveryCommand;
import com.nitchaya.notify.common.NotificationRequestedEvent;
import com.nitchaya.notify.common.NotificationStatus;
import com.nitchaya.notify.worker.config.KafkaConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationDispatcher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StatusPublisher statusPublisher;

    public NotificationDispatcher(KafkaTemplate<String, Object> kafkaTemplate, StatusPublisher statusPublisher) {
        this.kafkaTemplate = kafkaTemplate;
        this.statusPublisher = statusPublisher;
    }

    @KafkaListener(topics = KafkaConfig.REQUESTED, groupId = "notification-dispatcher", concurrency = "3")
    public void dispatch(NotificationRequestedEvent event) {
        statusPublisher.publish(event.notificationId(), NotificationStatus.PROCESSING, "Dispatching to channel workers");
        for (var channel : event.channels()) {
            var command = new DeliveryCommand(
                    event.notificationId(), event.recipient(), event.subject(), event.body(), channel,
                    event.channels().size(), event.metadata(), Instant.now());
            try {
                kafkaTemplate.send(KafkaConfig.DELIVERY,
                        event.notificationId() + ":" + channel.name(), command).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("Could not fan out delivery command", e);
            }
        }
    }
}
