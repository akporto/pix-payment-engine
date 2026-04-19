package com.pix.engine.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {

    Optional<String> get(UUID transactionId);

    void save(UUID transactionId, String payload);
}
