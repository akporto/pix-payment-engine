package com.pix.engine.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private Payment pendingPayment() {
        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Money(new BigDecimal("100.00"), "BRL")
        );
    }

    @Test
    void givenNewPayment_whenCreated_thenStatusIsPending() {
        Payment payment = pendingPayment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getProcessedAt()).isNull();
    }

    @Test
    void givenPendingPayment_whenCompleted_thenStatusIsCompletedAndProcessedAtIsSet() {
        Payment payment = pendingPayment();
        Instant before = Instant.now();

        payment.complete();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getProcessedAt()).isNotNull();
        assertThat(payment.getProcessedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void givenPendingPayment_whenFailed_thenStatusIsFailedAndProcessedAtIsSet() {
        Payment payment = pendingPayment();
        Instant before = Instant.now();

        payment.fail("Network timeout");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getProcessedAt()).isNotNull();
        assertThat(payment.getProcessedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void givenCompletedPayment_whenCompleteCalledAgain_thenThrowsIllegalStateException() {
        Payment payment = pendingPayment();
        payment.complete();

        assertThatThrownBy(payment::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot transition from status: COMPLETED");
    }

    @Test
    void givenCompletedPayment_whenFailCalled_thenThrowsIllegalStateException() {
        Payment payment = pendingPayment();
        payment.complete();

        assertThatThrownBy(() -> payment.fail("reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot transition from status: COMPLETED");
    }

    @Test
    void givenFailedPayment_whenCompleteCalled_thenThrowsIllegalStateException() {
        Payment payment = pendingPayment();
        payment.fail("error");

        assertThatThrownBy(payment::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot transition from status: FAILED");
    }

    @Test
    void givenPaymentWithFullConstructor_whenRead_thenFieldsArePreserved() {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Money amount = new Money(new BigDecimal("250.00"), "BRL");
        Instant createdAt = Instant.now();
        Instant processedAt = Instant.now();

        Payment payment = new Payment(id, transactionId, senderId, receiverId,
                amount, PaymentStatus.COMPLETED, createdAt, processedAt);

        assertThat(payment.getId()).isEqualTo(id);
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        assertThat(payment.getSenderAccountId()).isEqualTo(senderId);
        assertThat(payment.getReceiverAccountId()).isEqualTo(receiverId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getCreatedAt()).isEqualTo(createdAt);
        assertThat(payment.getProcessedAt()).isEqualTo(processedAt);
    }
}
