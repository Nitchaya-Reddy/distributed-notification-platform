package com.nitchaya.notify.common;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DeliveryCommand(
        UUID notificationId,
        String recipient,
        String subject,
        String body,
        NotificationChannel channel,
        int totalChannels,
        Map<String, String> metadata,
        Instant createdAt
) {}
