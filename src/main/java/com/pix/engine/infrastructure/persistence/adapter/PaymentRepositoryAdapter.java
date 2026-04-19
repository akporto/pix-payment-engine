package com.pix.engine.infrastructure.persistence.adapter;

import com.pix.engine.domain.model.Payment;
import com.pix.engine.domain.port.out.PaymentRepository;
import com.pix.engine.infrastructure.persistence.mapper.PaymentMapper;
import com.pix.engine.infrastructure.persistence.repository.PaymentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return PaymentMapper.toDomain(jpaRepository.save(PaymentMapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findByTransactionId(UUID transactionId) {
        return jpaRepository.findByTransactionId(transactionId).map(PaymentMapper::toDomain);
    }
}
