package com.pix.engine.infrastructure.persistence.mapper;

import com.pix.engine.domain.model.Money;
import com.pix.engine.domain.model.Payment;
import com.pix.engine.infrastructure.persistence.entity.PaymentEntity;

public class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentEntity toEntity(Payment payment) {
        return new PaymentEntity(
                payment.getId(),
                payment.getTransactionId(),
                payment.getSenderAccountId(),
                payment.getReceiverAccountId(),
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getProcessedAt()
        );
    }

    public static Payment toDomain(PaymentEntity entity) {
        Money amount = new Money(entity.getAmount(), entity.getCurrency());
        return new Payment(
                entity.getId(),
                entity.getTransactionId(),
                entity.getSenderAccountId(),
                entity.getReceiverAccountId(),
                amount,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getProcessedAt()
        );
    }
}
