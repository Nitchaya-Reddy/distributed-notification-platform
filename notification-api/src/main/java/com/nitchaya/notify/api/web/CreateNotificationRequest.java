package com.nitchaya.notify.api.web;

import com.nitchaya.notify.common.NotificationChannel;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record CreateNotificationRequest(
        @NotBlank @Size(max = 320) String recipient,
        @Size(max = 500) String subject,
        @NotBlank @Size(max = 10000) String body,
        @NotEmpty Set<NotificationChannel> channels,
        Instant scheduledAt,
        Map<String, String> metadata
) {}
