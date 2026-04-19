package com.pix.engine.infrastructure.persistence.adapter;

import com.pix.engine.domain.model.Account;
import com.pix.engine.domain.port.out.AccountRepository;
import com.pix.engine.infrastructure.persistence.entity.AccountEntity;
import com.pix.engine.infrastructure.persistence.mapper.AccountMapper;
import com.pix.engine.infrastructure.persistence.repository.AccountJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryAdapter(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id).map(AccountMapper::toDomain);
    }

    @Override
    public Account findByIdWithLock(UUID id) {
        AccountEntity entity = jpaRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        return AccountMapper.toDomain(entity);
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = AccountMapper.toEntity(account);
        AccountEntity saved = jpaRepository.save(entity);
        return AccountMapper.toDomain(saved);
    }
}
