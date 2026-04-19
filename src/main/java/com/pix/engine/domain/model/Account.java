package com.pix.engine.domain.model;

import com.pix.engine.domain.exception.InsufficientFundsException;

import java.util.UUID;

public class Account {

    private final UUID id;
    private final String pixKey;
    private Money balance;
    private final AccountStatus status;

    public Account(UUID id, String pixKey, Money balance, AccountStatus status) {
        this.id = id;
        this.pixKey = pixKey;
        this.balance = balance;
        this.status = status;
    }

    public void debit(Money amount) {
        if (!isActive()) {
            throw new IllegalStateException("Account is not active: " + id);
        }
        if (!balance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        this.balance = balance.subtract(amount);
    }

    public void credit(Money amount) {
        if (!isActive()) {
            throw new IllegalStateException("Account is not active: " + id);
        }
        this.balance = balance.add(amount);
    }

    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.status);
    }

    public UUID getId() {
        return id;
    }

    public String getPixKey() {
        return pixKey;
    }

    public Money getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
