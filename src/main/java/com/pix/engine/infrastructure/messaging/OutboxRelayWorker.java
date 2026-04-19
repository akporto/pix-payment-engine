package com.pix.engine.infrastructure.messaging;

import com.pix.engine.domain.model.OutboxEventStatus;
import com.pix.engine.infrastructure.persistence.entity.OutboxEntity;
import com.pix.engine.infrastructure.persistence.repository.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class OutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayWorker.class);

    private static final int BATCH_SIZE = 50;
    private static final String TOPIC = "pix.payments";

    private final OutboxJpaRepository outboxJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final TransactionTemplate transactionTemplate;

    public OutboxRelayWorker(OutboxJpaRepository outboxJpaRepository,
                             KafkaTemplate<String, String> kafkaTemplate,
                             PlatformTransactionManager transactionManager) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relay() {
        List<OutboxEntity> batch = outboxJpaRepository.findPendingEventsWithLock(BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        log.debug("Outbox relay: processing batch of {} events", batch.size());

        for (OutboxEntity event : batch) {
            kafkaTemplate
                    .send(TOPIC, event.getAggregateId().toString(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markAsProcessed(event);
                            log.debug("Outbox relay: event {} published and marked PROCESSED",
                                    event.getId());
                        } else {
                            log.error("Outbox relay: Kafka delivery failed for event {}. " +
                                    "Event will be retried. Cause: {}", event.getId(), ex.getMessage());
                        }
                    });
        }
    }

    private void markAsProcessed(OutboxEntity event) {
        transactionTemplate.execute(status ->
                outboxJpaRepository.findById(event.getId()).map(entity -> {
                    entity.setStatus(OutboxEventStatus.PROCESSED);
                    return outboxJpaRepository.save(entity);
                }).orElseGet(() -> {
                    log.warn("Outbox relay: event {} not found during PROCESSED update", event.getId());
                    return null;
                })
        );
    }
}
