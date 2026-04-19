package com.pix.engine.application.usecase;

import com.pix.engine.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    private static final String CACHE_KEY_PREFIX = "pix:idempotency:";

    private static final UUID SENDER_ID   = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_ID = UUID.fromString("b2000000-0000-0000-0000-000000000002");

    @Autowired
    ProcessPaymentUseCase useCase;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("INSERT INTO accounts (id, pix_key, balance, currency, status) VALUES (?,?,?,?,?)",
                SENDER_ID,   "sender2@test.com",   new BigDecimal("1000.00"), "BRL", "ACTIVE");
        jdbc.update("INSERT INTO accounts (id, pix_key, balance, currency, status) VALUES (?,?,?,?,?)",
                RECEIVER_ID, "receiver2@test.com", BigDecimal.ZERO,           "BRL", "ACTIVE");
    }

    @Test
    @DisplayName("First call populates Redis; second call hits cache and does not re-process")
    void givenFirstPaymentProcessed_whenRetried_thenRedisCacheServedWithoutDbWrite() {
        UUID transactionId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                transactionId, SENDER_ID, RECEIVER_ID, new BigDecimal("50.00"));

        ProcessPaymentResult first = useCase.execute(command);
        assertThat(first.created()).isTrue();

        String cacheKey = CACHE_KEY_PREFIX + transactionId;
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
        assertThat(redisTemplate.getExpire(cacheKey)).isPositive();

        ProcessPaymentResult second = useCase.execute(command);
        assertThat(second.created()).isFalse();
        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(second.status()).isEqualTo("COMPLETED");

        BigDecimal senderBalance = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, SENDER_ID);
        assertThat(senderBalance).isEqualByComparingTo("950.00");

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE transaction_id = ?",
                Integer.class, transactionId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Graceful degradation: Redis evicted (simulating restart) falls back to DB idempotency")
    void givenRedisFlushedAfterPayment_whenRetried_thenFallsBackToDatabaseAndReturnsCorrectResult() {
        UUID transactionId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                transactionId, SENDER_ID, RECEIVER_ID, new BigDecimal("75.00"));

        ProcessPaymentResult first = useCase.execute(command);

        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + transactionId)).isFalse();

        ProcessPaymentResult second = useCase.execute(command);
        assertThat(second.created()).isFalse();
        assertThat(second.paymentId()).isEqualTo(first.paymentId());

        BigDecimal senderBalance = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, SENDER_ID);
        assertThat(senderBalance).isEqualByComparingTo("925.00");

        Integer paymentRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE transaction_id = ?",
                Integer.class, transactionId);
        assertThat(paymentRows).isEqualTo(1);

        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + transactionId)).isTrue();
    }

    @Test
    @DisplayName("Redis MISS + payment already in Postgres: pre-transaction fast path, no duplicate payment")
    void givenPaymentCommitted_whenRedisEmptyOnRetry_thenReadOnlyIdempotencyWithoutExtraDebit() {
        UUID transactionId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                transactionId, SENDER_ID, RECEIVER_ID, new BigDecimal("40.00"));

        ProcessPaymentResult first = useCase.execute(command);
        assertThat(first.created()).isTrue();

        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });

        ProcessPaymentResult second = useCase.execute(command);
        assertThat(second.created()).isFalse();
        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(second.status()).isEqualTo("COMPLETED");

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE transaction_id = ?",
                Integer.class, transactionId)).isEqualTo(1);

        BigDecimal senderBalance = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, SENDER_ID);
        assertThat(senderBalance).isEqualByComparingTo("960.00");

        BigDecimal receiverBalance = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, RECEIVER_ID);
        assertThat(receiverBalance).isEqualByComparingTo("40.00");

        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + transactionId)).isTrue();
    }
}
