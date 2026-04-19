package com.pix.engine.infrastructure.cache;

import com.pix.engine.domain.port.out.IdempotencyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisIdempotencyAdapter implements IdempotencyRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyAdapter.class);

    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "pix:idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<String> get(UUID transactionId) {
        try {
            String cached = redisTemplate.opsForValue().get(KEY_PREFIX + transactionId);
            if (cached != null) {
                meterRegistry.counter("pix.payment.idempotency.check", "result", "hit").increment();
                return Optional.of(cached);
            }
            meterRegistry.counter("pix.payment.idempotency.check", "result", "miss").increment();
            return Optional.empty();
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.warn("Redis unavailable on idempotency check, falling back to database. Cause: {}",
                    ex.getMessage());
            meterRegistry.counter("pix.payment.idempotency.check", "result", "miss").increment();
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Unexpected Redis error on idempotency check, falling back to database. Cause: {}",
                    ex.getMessage());
            meterRegistry.counter("pix.payment.idempotency.check", "result", "miss").increment();
            return Optional.empty();
        }
    }

    @Override
    public void save(UUID transactionId, String payload) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + transactionId, payload, TTL);
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.warn("Redis unavailable on idempotency save — payment already committed to DB. Cause: {}",
                    ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected Redis error on idempotency save — payment already committed to DB. Cause: {}",
                    ex.getMessage());
        }
    }
}
