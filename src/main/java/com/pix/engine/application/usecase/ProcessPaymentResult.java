package com.pix.engine.application.usecase;

import java.util.UUID;

public record ProcessPaymentResult(
        UUID paymentId,
        UUID transactionId,
        String status,
        boolean created
) {}
