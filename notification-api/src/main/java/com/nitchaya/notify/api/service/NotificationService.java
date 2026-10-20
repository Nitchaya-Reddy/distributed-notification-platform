package com.nitchaya.notify.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitchaya.notify.api.config.KafkaTopics;
import com.nitchaya.notify.api.domain.NotificationEntity;
import com.nitchaya.notify.api.domain.OutboxEntity;
import com.nitchaya.notify.api.repository.NotificationRepository;
import com.nitchaya.notify.api.repository.OutboxRepository;
import com.nitchaya.notify.api.web.CreateNotificationRequest;
import com.nitchaya.notify.common.NotificationRequestedEvent;
import com.nitchaya.notify.common.NotificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notifications, OutboxRepository outbox, ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NotificationEntity create(CreateNotificationRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = notifications.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) return existing.get();
        }

        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        Instant availableAt = request.scheduledAt() != null && request.scheduledAt().isAfter(now)
                ? request.scheduledAt() : now;
        NotificationStatus initialStatus = availableAt.isAfter(now) ? NotificationStatus.SCHEDULED : NotificationStatus.QUEUED;
        String channels = request.channels().stream().map(Enum::name).sorted().collect(Collectors.joining(","));

        NotificationEntity entity = new NotificationEntity(
                id, request.recipient(), request.subject(), request.body(), channels,
                initialStatus, normalizeKey(idempotencyKey), request.scheduledAt(), now);
        notifications.save(entity);

        NotificationRequestedEvent event = new NotificationRequestedEvent(
                id, request.recipient(), request.subject(), request.body(), request.channels(),
                request.metadata() == null ? Collections.emptyMap() : request.metadata(), now);
        try {
            outbox.save(new OutboxEntity(UUID.randomUUID(), id, KafkaTopics.REQUESTED,
                    objectMapper.writeValueAsString(event), availableAt, now));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize notification event", e);
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public NotificationEntity get(UUID id) {
        return notifications.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.length() > 120) throw new IllegalArgumentException("Idempotency-Key must be <= 120 characters");
        return key;
    }
}
