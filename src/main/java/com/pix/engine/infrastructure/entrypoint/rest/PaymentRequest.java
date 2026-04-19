package com.pix.engine.infrastructure.entrypoint.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(

        @NotNull(message = "senderAccountId is required")
        UUID senderAccountId,

        @NotNull(message = "receiverAccountId is required")
        UUID receiverAccountId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        BigDecimal amount
) {}
