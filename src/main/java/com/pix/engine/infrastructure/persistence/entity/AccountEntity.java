package com.pix.engine.infrastructure.persistence.entity;

import com.pix.engine.domain.model.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "pix_key", nullable = false, unique = true)
    private String pixKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    protected AccountEntity() {}

    public AccountEntity(UUID id, String pixKey, BigDecimal balance,
                         String currency, AccountStatus status) {
        this.id = id;
        this.pixKey = pixKey;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getPixKey() { return pixKey; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
