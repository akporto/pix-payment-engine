package com.pix.engine.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void givenValidAmountAndCurrency_whenCreated_thenSucceeds() {
        Money money = new Money(new BigDecimal("100.00"), "BRL");
        assertThat(money.amount()).isEqualByComparingTo("100.00");
        assertThat(money.currency()).isEqualTo("BRL");
    }

    @Test
    void givenZeroAmount_whenCreated_thenSucceeds() {
        Money money = new Money(BigDecimal.ZERO, "BRL");
        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void givenNegativeAmount_whenCreated_thenThrowsIllegalArgument() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-0.01"), "BRL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be non-negative");
    }

    @Test
    void givenNullAmount_whenCreated_thenThrowsIllegalArgument() {
        assertThatThrownBy(() -> new Money(null, "BRL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be non-negative");
    }

    @Test
    void givenNullCurrency_whenCreated_thenThrowsIllegalArgument() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency must not be blank");
    }

    @Test
    void givenBlankCurrency_whenCreated_thenThrowsIllegalArgument() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency must not be blank");
    }

    @Test
    void givenSameCurrency_whenAdded_thenReturnsSum() {
        Money a = new Money(new BigDecimal("100.00"), "BRL");
        Money b = new Money(new BigDecimal("50.00"), "BRL");
        Money result = a.add(b);
        assertThat(result.amount()).isEqualByComparingTo("150.00");
        assertThat(result.currency()).isEqualTo("BRL");
    }

    @Test
    void givenSameCurrency_whenSubtracted_thenReturnsDifference() {
        Money a = new Money(new BigDecimal("100.00"), "BRL");
        Money b = new Money(new BigDecimal("30.00"), "BRL");
        Money result = a.subtract(b);
        assertThat(result.amount()).isEqualByComparingTo("70.00");
    }

    @Test
    void givenDifferentCurrencies_whenAdded_thenThrowsIllegalArgument() {
        Money brl = new Money(new BigDecimal("100.00"), "BRL");
        Money usd = new Money(new BigDecimal("10.00"), "USD");
        assertThatThrownBy(() -> brl.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void givenDifferentCurrencies_whenSubtracted_thenThrowsIllegalArgument() {
        Money brl = new Money(new BigDecimal("100.00"), "BRL");
        Money usd = new Money(new BigDecimal("10.00"), "USD");
        assertThatThrownBy(() -> brl.subtract(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void givenEqualAmounts_whenIsGreaterThanOrEqualTo_thenReturnsTrue() {
        Money a = new Money(new BigDecimal("100.00"), "BRL");
        Money b = new Money(new BigDecimal("100.00"), "BRL");
        assertThat(a.isGreaterThanOrEqualTo(b)).isTrue();
    }

    @Test
    void givenGreaterAmount_whenIsGreaterThanOrEqualTo_thenReturnsTrue() {
        Money a = new Money(new BigDecimal("200.00"), "BRL");
        Money b = new Money(new BigDecimal("100.00"), "BRL");
        assertThat(a.isGreaterThanOrEqualTo(b)).isTrue();
    }

    @Test
    void givenSmallerAmount_whenIsGreaterThanOrEqualTo_thenReturnsFalse() {
        Money a = new Money(new BigDecimal("50.00"), "BRL");
        Money b = new Money(new BigDecimal("100.00"), "BRL");
        assertThat(a.isGreaterThanOrEqualTo(b)).isFalse();
    }

    @Test
    void givenDifferentCurrencies_whenIsGreaterThanOrEqualTo_thenThrowsIllegalArgument() {
        Money brl = new Money(new BigDecimal("100.00"), "BRL");
        Money usd = new Money(new BigDecimal("10.00"), "USD");
        assertThatThrownBy(() -> brl.isGreaterThanOrEqualTo(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }
}
