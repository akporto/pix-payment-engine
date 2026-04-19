package com.pix.engine.application.usecase;

import com.pix.engine.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessPaymentUseCaseIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SENDER_ID   = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_ID = UUID.fromString("a2000000-0000-0000-0000-000000000002");

    @Autowired
    ProcessPaymentUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("INSERT INTO accounts (id, pix_key, balance, currency, status) VALUES (?,?,?,?,?)",
                SENDER_ID,   "sender@test.com",   new BigDecimal("1000.00"), "BRL", "ACTIVE");
        jdbc.update("INSERT INTO accounts (id, pix_key, balance, currency, status) VALUES (?,?,?,?,?)",
                RECEIVER_ID, "receiver@test.com", BigDecimal.ZERO,           "BRL", "ACTIVE");
    }

    @Test
    @DisplayName("New payment: balances updated, payment persisted as COMPLETED, OutboxEvent created as PENDING")
    void givenValidAccounts_whenPaymentExecuted_thenStateIsConsistent() {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                UUID.randomUUID(), SENDER_ID, RECEIVER_ID, new BigDecimal("250.00"));

        ProcessPaymentResult result = useCase.execute(command);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.created()).isTrue();

        BigDecimal senderBalance   = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, SENDER_ID);
        BigDecimal receiverBalance = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, RECEIVER_ID);

        assertThat(senderBalance).isEqualByComparingTo("750.00");
        assertThat(receiverBalance).isEqualByComparingTo("250.00");

        Integer outboxCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING'", Integer.class);
        assertThat(outboxCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Idempotency via DB: same transactionId processed twice results in exactly one payment and one balance change")
    void givenFirstPaymentCommitted_whenSameTransactionIdRetried_thenNoDuplicateProcessing() {
        UUID transactionId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                transactionId, SENDER_ID, RECEIVER_ID, new BigDecimal("100.00"));

        ProcessPaymentResult first  = useCase.execute(command);
        ProcessPaymentResult second = useCase.execute(command);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.paymentId()).isEqualTo(first.paymentId());

        Integer paymentCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE transaction_id = ?", Integer.class, transactionId);
        assertThat(paymentCount).isEqualTo(1);

        BigDecimal senderBalance = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, SENDER_ID);
        assertThat(senderBalance).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("Insufficient funds: domain throws exception, no payment or outbox event persisted")
    void givenInsufficientFunds_whenPaymentExecuted_thenExceptionThrownAndNothingPersisted() {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                UUID.randomUUID(), SENDER_ID, RECEIVER_ID, new BigDecimal("9999.00"));

        assertThatThrownBy(() -> useCase.execute(command))
                .hasMessageContaining("Insufficient funds");

        Integer paymentCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments", Integer.class);
        Integer outboxCount  = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(paymentCount).isZero();
        assertThat(outboxCount).isZero();

        BigDecimal senderBalance = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, SENDER_ID);
        assertThat(senderBalance).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Atomicity: OutboxEvent is always in the same transaction as the payment — both exist or neither does")
    void givenValidPayment_whenExecuted_thenOutboxEventSharesTransactionWithPayment() {
        UUID transactionId = UUID.randomUUID();
        useCase.execute(new ProcessPaymentCommand(
                transactionId, SENDER_ID, RECEIVER_ID, new BigDecimal("50.00")));

        UUID paymentId = jdbc.queryForObject(
                "SELECT id FROM payments WHERE transaction_id = ?", UUID.class, transactionId);
        UUID outboxAggregateId = jdbc.queryForObject(
                "SELECT aggregate_id FROM outbox_events", UUID.class);

        assertThat(outboxAggregateId).isEqualTo(paymentId);
    }
}
