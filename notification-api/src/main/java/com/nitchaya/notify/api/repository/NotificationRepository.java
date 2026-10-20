package com.nitchaya.notify.api.repository;

import com.nitchaya.notify.api.domain.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey);
}
