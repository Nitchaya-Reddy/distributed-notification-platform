package com.nitchaya.notify.common;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record NotificationRequestedEvent(
        UUID notificationId,
        String recipient,
        String subject,
        String body,
        Set<NotificationChannel> channels,
        Map<String, String> metadata,
        Instant createdAt
) {}
