package com.pix.engine.domain.port.out;

import com.pix.engine.domain.model.OutboxEvent;

public interface OutboxRepository {

    void save(OutboxEvent event);
}
