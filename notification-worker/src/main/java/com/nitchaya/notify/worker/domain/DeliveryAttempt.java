package com.nitchaya.notify.worker.domain;

import com.nitchaya.notify.common.NotificationChannel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts", uniqueConstraints = @UniqueConstraint(
        name = "uq_delivery_notification_channel", columnNames = {"notification_id", "channel"}))
public class DeliveryAttempt {
    @Id
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeliveryAttempt() {}

    public static DeliveryAttempt pending(UUID notificationId, NotificationChannel channel) {
        var now = Instant.now();
        var attempt = new DeliveryAttempt();
        attempt.id = UUID.randomUUID();
        attempt.notificationId = notificationId;
        attempt.channel = channel;
        attempt.status = DeliveryStatus.PENDING;
        attempt.createdAt = now;
        attempt.updatedAt = now;
        return attempt;
    }

    public UUID getNotificationId() { return notificationId; }
    public NotificationChannel getChannel() { return channel; }
    public DeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getProviderMessageId() { return providerMessageId; }

    public void markAttempt() {
        attemptCount++;
        status = DeliveryStatus.PROCESSING;
        updatedAt = Instant.now();
    }

    public void markSent(String providerMessageId) {
        this.status = DeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = DeliveryStatus.FAILED;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
        this.updatedAt = Instant.now();
    }
}
