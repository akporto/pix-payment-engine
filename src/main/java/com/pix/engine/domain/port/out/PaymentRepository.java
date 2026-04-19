package com.pix.engine.domain.port.out;

import com.pix.engine.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByTransactionId(UUID transactionId);
}
