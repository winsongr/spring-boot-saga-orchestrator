CREATE TABLE outbox (
    id           BIGSERIAL   PRIMARY KEY,
    saga_id      UUID        NOT NULL,
    topic        TEXT        NOT NULL,
    key          TEXT        NOT NULL,
    payload      JSONB       NOT NULL,
    headers      JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMPTZ,
    attempts     INT         NOT NULL DEFAULT 0
);

-- publisher hot path: find unsent in FIFO order
CREATE INDEX idx_outbox_unsent_created
    ON outbox (created_at)
    WHERE sent_at IS NULL;
