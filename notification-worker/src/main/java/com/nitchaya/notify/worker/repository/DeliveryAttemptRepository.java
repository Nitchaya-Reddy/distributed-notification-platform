package com.nitchaya.notify.worker.repository;

import com.nitchaya.notify.common.NotificationChannel;
import com.nitchaya.notify.worker.domain.DeliveryAttempt;
import com.nitchaya.notify.worker.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {
    Optional<DeliveryAttempt> findByNotificationIdAndChannel(UUID notificationId, NotificationChannel channel);
    long countByNotificationIdAndStatus(UUID notificationId, DeliveryStatus status);
}
