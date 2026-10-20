package com.nitchaya.notify.api.web;

import com.nitchaya.notify.api.domain.NotificationEntity;
import com.nitchaya.notify.common.NotificationStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipient,
        String subject,
        String body,
        List<String> channels,
        NotificationStatus status,
        Instant scheduledAt,
        String message,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificationResponse from(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(), entity.getRecipient(), entity.getSubject(), entity.getBody(),
                Arrays.stream(entity.getChannels().split(",")).toList(), entity.getStatus(),
                entity.getScheduledAt(), entity.getLastMessage(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
