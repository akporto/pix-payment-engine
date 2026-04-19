package com.pix.engine.domain.model;

import com.pix.engine.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final UUID ID = UUID.randomUUID();

    private Account activeAccount(String balance) {
        return new Account(ID, "test@pix.com",
                new Money(new BigDecimal(balance), "BRL"), AccountStatus.ACTIVE);
    }

    private Account inactiveAccount() {
        return new Account(ID, "test@pix.com",
                new Money(new BigDecimal("100.00"), "BRL"), AccountStatus.INACTIVE);
    }

    @Test
    void givenActiveAccount_whenDebited_thenBalanceDecreases() {
        Account account = activeAccount("200.00");
        account.debit(new Money(new BigDecimal("80.00"), "BRL"));
        assertThat(account.getBalance().amount()).isEqualByComparingTo("120.00");
    }

    @Test
    void givenActiveAccount_whenCredited_thenBalanceIncreases() {
        Account account = activeAccount("100.00");
        account.credit(new Money(new BigDecimal("50.00"), "BRL"));
        assertThat(account.getBalance().amount()).isEqualByComparingTo("150.00");
    }

    @Test
    void givenBalanceEqualToDebit_whenDebited_thenBalanceBecomesZero() {
        Account account = activeAccount("100.00");
        account.debit(new Money(new BigDecimal("100.00"), "BRL"));
        assertThat(account.getBalance().amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void givenInsufficientBalance_whenDebited_thenThrowsInsufficientFundsException() {
        Account account = activeAccount("50.00");
        Money debit = new Money(new BigDecimal("100.00"), "BRL");
        assertThatThrownBy(() -> account.debit(debit))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void givenInactiveAccount_whenDebited_thenThrowsIllegalStateException() {
        Account account = inactiveAccount();
        Money debit = new Money(new BigDecimal("10.00"), "BRL");
        assertThatThrownBy(() -> account.debit(debit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account is not active");
    }

    @Test
    void givenInactiveAccount_whenCredited_thenThrowsIllegalStateException() {
        Account account = inactiveAccount();
        Money credit = new Money(new BigDecimal("10.00"), "BRL");
        assertThatThrownBy(() -> account.credit(credit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account is not active");
    }

    @Test
    void givenActiveAccount_whenIsActiveChecked_thenReturnsTrue() {
        assertThat(activeAccount("0.00").isActive()).isTrue();
    }

    @Test
    void givenInactiveAccount_whenIsActiveChecked_thenReturnsFalse() {
        assertThat(inactiveAccount().isActive()).isFalse();
    }
}
