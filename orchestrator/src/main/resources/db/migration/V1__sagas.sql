CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE sagas (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type         TEXT        NOT NULL,
    status       TEXT        NOT NULL CHECK (status IN
                 ('PENDING','RUNNING','COMPENSATING','COMPLETED','FAILED','STUCK')),
    payload      JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_sagas_status_updated ON sagas (status, updated_at DESC);
CREATE INDEX idx_sagas_type_created   ON sagas (type, created_at DESC);
