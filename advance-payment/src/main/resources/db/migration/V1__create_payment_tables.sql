-- advance-payment V1: платежи и Outbox

CREATE TABLE payments (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    advance_id        UUID          UNIQUE NOT NULL,
    amount            NUMERIC(12,2) NOT NULL,
    recipient_id      UUID          NOT NULL,
    recipient_phone   VARCHAR(20),
    sbp_request_id    VARCHAR(100)  UNIQUE,
    sbp_response_code VARCHAR(20),
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    error_message     TEXT,
    initiated_at      TIMESTAMPTZ   DEFAULT NOW(),
    completed_at      TIMESTAMPTZ
);

CREATE INDEX idx_payments_advance ON payments(advance_id);
CREATE INDEX idx_payments_status  ON payments(status);

CREATE TABLE outbox_events (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'ADVANCE',
    aggregate_id   UUID        NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    status         VARCHAR(20)  DEFAULT 'PENDING',
    retry_count    INTEGER      DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMPTZ  DEFAULT NOW(),
    processed_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
