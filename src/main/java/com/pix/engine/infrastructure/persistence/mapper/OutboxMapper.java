package com.pix.engine.infrastructure.persistence.mapper;

import com.pix.engine.domain.model.OutboxEvent;
import com.pix.engine.infrastructure.persistence.entity.OutboxEntity;

public class OutboxMapper {

    private OutboxMapper() {}

    public static OutboxEntity toEntity(OutboxEvent event) {
        return new OutboxEntity(
                event.getId(),
                event.getAggregateId(),
                event.getType(),
                event.getPayload(),
                event.getStatus(),
                event.getCreatedAt()
        );
    }

    public static OutboxEvent toDomain(OutboxEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                entity.getType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
