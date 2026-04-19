CREATE TABLE IF NOT EXISTS accounts (
    id          UUID            NOT NULL,
    pix_key     VARCHAR(255)    NOT NULL,
    balance     NUMERIC(19, 2)  NOT NULL,
    currency    VARCHAR(3)      NOT NULL,
    status      VARCHAR(20)     NOT NULL,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uk_accounts_pix_key UNIQUE (pix_key)
);

CREATE TABLE IF NOT EXISTS payments (
    id                  UUID            NOT NULL,
    transaction_id      UUID            NOT NULL,
    sender_account_id   UUID            NOT NULL,
    receiver_account_id UUID            NOT NULL,
    amount              NUMERIC(19, 2)  NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    processed_at        TIMESTAMPTZ,

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_transaction_id UNIQUE (transaction_id)
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id           UUID        NOT NULL,
    aggregate_id UUID        NOT NULL,
    type         VARCHAR(100) NOT NULL,
    payload      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created_at
    ON outbox_events (status, created_at ASC)
    WHERE status = 'PENDING';
