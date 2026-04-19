package com.pix.engine.infrastructure.persistence.adapter;

import com.pix.engine.domain.model.OutboxEvent;
import com.pix.engine.domain.port.out.OutboxRepository;
import com.pix.engine.infrastructure.persistence.mapper.OutboxMapper;
import com.pix.engine.infrastructure.persistence.repository.OutboxJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class OutboxRepositoryAdapter implements OutboxRepository {

    private final OutboxJpaRepository jpaRepository;

    public OutboxRepositoryAdapter(OutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(OutboxEvent event) {
        jpaRepository.save(OutboxMapper.toEntity(event));
    }
}
