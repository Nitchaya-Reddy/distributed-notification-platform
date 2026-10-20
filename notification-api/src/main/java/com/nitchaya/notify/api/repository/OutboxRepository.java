package com.nitchaya.notify.api.repository;

import com.nitchaya.notify.api.domain.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {
    @Query(value = """
            SELECT * FROM notification_outbox
            WHERE status = 'PENDING' AND available_at <= CURRENT_TIMESTAMP
            ORDER BY available_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<OutboxEntity> lockNextBatch(@Param("batchSize") int batchSize);
}
