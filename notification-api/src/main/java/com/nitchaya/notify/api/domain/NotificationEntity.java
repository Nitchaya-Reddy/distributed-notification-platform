package com.nitchaya.notify.api.domain;

import com.nitchaya.notify.common.NotificationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, length = 100)
    private String channels;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "idempotency_key", unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "last_message", length = 1000)
    private String lastMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected NotificationEntity() {}

    public NotificationEntity(UUID id, String recipient, String subject, String body, String channels,
                              NotificationStatus status, String idempotencyKey, Instant scheduledAt,
                              Instant createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.channels = channels;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.scheduledAt = scheduledAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getChannels() { return channels; }
    public NotificationStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getLastMessage() { return lastMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateStatus(NotificationStatus status, String message, Instant at) {
        if ((this.status == NotificationStatus.SENT || this.status == NotificationStatus.FAILED)
                && this.status != status) {
            return;
        }
        this.status = status;
        this.lastMessage = message;
        this.updatedAt = at;
    }
}
