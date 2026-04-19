package com.pix.engine.domain.port.out;

import com.pix.engine.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findById(UUID id);

    Account findByIdWithLock(UUID id);

    Account save(Account account);
}
