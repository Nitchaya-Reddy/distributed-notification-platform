package com.nitchaya.notify.common;

import java.time.Instant;
import java.util.UUID;

public record NotificationStatusEvent(
        UUID notificationId,
        NotificationStatus status,
        String message,
        Instant updatedAt
) {}
