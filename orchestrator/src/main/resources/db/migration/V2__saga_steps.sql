CREATE TABLE saga_steps (
    saga_id           UUID        NOT NULL REFERENCES sagas(id) ON DELETE CASCADE,
    step_name         TEXT        NOT NULL,
    sequence_no       INT         NOT NULL,
    status            TEXT        NOT NULL CHECK (status IN
                      ('PENDING','RUNNING','COMPLETED','FAILED','TIMED_OUT',
                       'COMPENSATING','COMPENSATED','COMPENSATION_FAILED')),
    request_payload   JSONB       NOT NULL,
    response_payload  JSONB,
    error_message     TEXT,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    deadline_at       TIMESTAMPTZ,
    attempt_count     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (saga_id, step_name)
);

-- timeout sweeper queries this hot path
CREATE INDEX idx_saga_steps_running_deadline
    ON saga_steps (deadline_at)
    WHERE status IN ('RUNNING','COMPENSATING');
