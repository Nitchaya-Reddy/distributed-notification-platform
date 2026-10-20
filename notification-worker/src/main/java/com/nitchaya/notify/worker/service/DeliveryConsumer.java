package com.nitchaya.notify.worker.service;

import com.nitchaya.notify.common.DeliveryCommand;
import com.nitchaya.notify.common.NotificationStatus;
import com.nitchaya.notify.worker.config.KafkaConfig;
import com.nitchaya.notify.worker.domain.DeliveryStatus;
import com.nitchaya.notify.worker.provider.ProviderRegistry;
import com.nitchaya.notify.worker.repository.DeliveryAttemptRepository;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
public class DeliveryConsumer {
    private final ProviderRegistry providers;
    private final RedisRateLimiter rateLimiter;
    private final DeliveryAttemptWriter attempts;
    private final DeliveryAttemptRepository repository;
    private final StatusPublisher statusPublisher;

    public DeliveryConsumer(ProviderRegistry providers, RedisRateLimiter rateLimiter,
                            DeliveryAttemptWriter attempts, DeliveryAttemptRepository repository,
                            StatusPublisher statusPublisher) {
        this.providers = providers;
        this.rateLimiter = rateLimiter;
        this.attempts = attempts;
        this.repository = repository;
        this.statusPublisher = statusPublisher;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 8000),
            autoCreateTopics = "true",
            numPartitions = "12")
    @KafkaListener(topics = KafkaConfig.DELIVERY, groupId = "notification-delivery-workers", concurrency = "6")
    public void deliver(DeliveryCommand command) {
        var attempt = attempts.start(command);
        if (attempt.getStatus() == DeliveryStatus.SENT) return;

        try {
            rateLimiter.check(command.recipient(), command.channel());
            String providerId = providers.get(command.channel()).send(command);
            attempts.sent(attempt, providerId);

            long sent = repository.countByNotificationIdAndStatus(command.notificationId(), DeliveryStatus.SENT);
            if (sent >= command.totalChannels()) {
                statusPublisher.publish(command.notificationId(), NotificationStatus.SENT,
                        "Delivered successfully to all requested channels");
            } else {
                statusPublisher.publish(command.notificationId(), NotificationStatus.PROCESSING,
                        "Delivered " + command.channel() + "; waiting for remaining channels");
            }
        } catch (RuntimeException e) {
            attempts.failed(command, e.getMessage());
            throw e;
        }
    }

    @DltHandler
    public void onDeadLetter(DeliveryCommand command) {
        attempts.failed(command, "Retries exhausted; routed to dead-letter topic");
        statusPublisher.publish(command.notificationId(), NotificationStatus.FAILED,
                "Delivery failed for channel " + command.channel() + " after retries");
    }
}
