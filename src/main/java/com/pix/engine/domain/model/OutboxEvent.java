package com.pix.engine.domain.model;

import java.time.Instant;
import java.util.UUID;

public class OutboxEvent {

    private final UUID id;
    private final UUID aggregateId;
    private final String type;
    private final String payload;
    private OutboxEventStatus status;
    private final Instant createdAt;

    public OutboxEvent(UUID id, UUID aggregateId, String type, String payload) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public OutboxEvent(UUID id, UUID aggregateId, String type, String payload,
                       OutboxEventStatus status, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void markAsProcessed() {
        if (!OutboxEventStatus.PENDING.equals(this.status)) {
            throw new IllegalStateException(
                    "OutboxEvent " + id + " has already been processed");
        }
        this.status = OutboxEventStatus.PROCESSED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
