package com.pix.engine.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentCommand(
        UUID transactionId,
        UUID senderAccountId,
        UUID receiverAccountId,
        BigDecimal amount
) {}
