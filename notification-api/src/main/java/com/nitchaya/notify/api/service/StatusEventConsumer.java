package com.nitchaya.notify.api.service;

import com.nitchaya.notify.api.config.KafkaTopics;
import com.nitchaya.notify.api.repository.NotificationRepository;
import com.nitchaya.notify.common.NotificationStatusEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusEventConsumer {
    private final NotificationRepository repository;

    public StatusEventConsumer(NotificationRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = KafkaTopics.STATUS, groupId = "notification-api-status", concurrency = "3")
    @Transactional
    public void onStatus(NotificationStatusEvent event) {
        repository.findById(event.notificationId())
                .ifPresent(entity -> entity.updateStatus(event.status(), event.message(), event.updatedAt()));
    }
}
