package com.pix.engine.infrastructure.persistence.mapper;

import com.pix.engine.domain.model.Account;
import com.pix.engine.domain.model.Money;
import com.pix.engine.infrastructure.persistence.entity.AccountEntity;

public class AccountMapper {

    private AccountMapper() {}

    public static AccountEntity toEntity(Account account) {
        return new AccountEntity(
                account.getId(),
                account.getPixKey(),
                account.getBalance().amount(),
                account.getBalance().currency(),
                account.getStatus()
        );
    }

    public static Account toDomain(AccountEntity entity) {
        Money balance = new Money(entity.getBalance(), entity.getCurrency());
        return new Account(entity.getId(), entity.getPixKey(), balance, entity.getStatus());
    }
}
