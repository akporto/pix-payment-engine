package com.pix.engine.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final UUID transactionId;
    private final UUID senderAccountId;
    private final UUID receiverAccountId;
    private final Money amount;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant processedAt;

    public Payment(UUID id, UUID transactionId, UUID senderAccountId,
                   UUID receiverAccountId, Money amount) {
        this.id = id;
        this.transactionId = transactionId;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Payment(UUID id, UUID transactionId, UUID senderAccountId,
                   UUID receiverAccountId, Money amount, PaymentStatus status,
                   Instant createdAt, Instant processedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public void complete() {
        assertPending();
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = Instant.now();
    }

    public void fail(String reason) {
        assertPending();
        this.status = PaymentStatus.FAILED;
        this.processedAt = Instant.now();
    }

    private void assertPending() {
        if (!PaymentStatus.PENDING.equals(this.status)) {
            throw new IllegalStateException(
                    "Payment " + id + " cannot transition from status: " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public UUID getReceiverAccountId() {
        return receiverAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
