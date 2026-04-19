package com.pix.engine.domain.exception;

import com.pix.engine.domain.model.Money;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID accountId, Money available, Money requested) {
        super(String.format(
                "Insufficient funds on account %s: available %s %s, requested %s %s",
                accountId,
                available.amount(), available.currency(),
                requested.amount(), requested.currency()));
    }
}
