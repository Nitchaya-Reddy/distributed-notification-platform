CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(500),
    body TEXT NOT NULL,
    channels VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(120) UNIQUE,
    scheduled_at TIMESTAMPTZ,
    last_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_outbox_ready ON notification_outbox(status, available_at);

CREATE TABLE delivery_attempts (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(200),
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_delivery_notification_channel UNIQUE(notification_id, channel)
);

CREATE INDEX idx_delivery_notification ON delivery_attempts(notification_id);
