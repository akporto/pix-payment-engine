package com.pix.engine.infrastructure.persistence.repository;

import com.pix.engine.infrastructure.persistence.entity.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID> {

    @Query(
        value = "SELECT * FROM outbox_events WHERE status = 'PENDING' " +
                "ORDER BY created_at ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED",
        nativeQuery = true
    )
    List<OutboxEntity> findPendingEventsWithLock(@Param("batchSize") int batchSize);
}
