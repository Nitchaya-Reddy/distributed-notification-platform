package com.nitchaya.notify.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
public class OutboxEntity {
    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 80)
    private String topic;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutboxEntity() {}

    public OutboxEntity(UUID id, UUID aggregateId, String topic, String payload, Instant availableAt, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.payload = payload;
        this.availableAt = availableAt;
        this.createdAt = createdAt;
        this.status = "PENDING";
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public Instant getAvailableAt() { return availableAt; }
    public Instant getPublishedAt() { return publishedAt; }

    public void markPublished(Instant at) {
        this.status = "PUBLISHED";
        this.publishedAt = at;
    }
}
