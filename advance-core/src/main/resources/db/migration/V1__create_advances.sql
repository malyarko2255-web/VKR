-- advance-core V1: таблицы заявок на авансирование

-- Последовательность для нумерации заявок
CREATE SEQUENCE advance_request_seq START 1 INCREMENT 1;

CREATE TABLE advances (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    request_no          VARCHAR(20)   UNIQUE NOT NULL,
    driver_id           UUID          NOT NULL,
    route_id            UUID,
    trip_id             UUID,
    trip_stage          VARCHAR(20)   NOT NULL,
    advance_type        VARCHAR(30)   NOT NULL,
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    limit_available     NUMERIC(12,2),
    auto_approved       BOOLEAN       DEFAULT FALSE,
    score_at_creation   INTEGER,
    dispatcher_id       UUID,
    dispatcher_at       TIMESTAMPTZ,
    dispatcher_comment  TEXT,
    finance_id          UUID,
    finance_at          TIMESTAMPTZ,
    finance_comment     TEXT,
    rejection_reason    TEXT,
    notes               TEXT,
    created_at          TIMESTAMPTZ   DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   DEFAULT NOW(),
    version             INTEGER       DEFAULT 0
);

CREATE INDEX idx_advances_driver_status ON advances(driver_id, status);
CREATE INDEX idx_advances_status        ON advances(status);
CREATE INDEX idx_advances_created_at    ON advances(created_at DESC);

CREATE TABLE advance_history (
    id          BIGSERIAL   PRIMARY KEY,
    advance_id  UUID        NOT NULL REFERENCES advances(id),
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    changed_by  UUID,
    comment     TEXT,
    changed_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_advance_history_advance ON advance_history(advance_id);

-- Функция автогенерации request_no формата A-YYYY-NNNNN
CREATE OR REPLACE FUNCTION generate_request_no()
RETURNS TRIGGER AS $$
BEGIN
    NEW.request_no := 'A-' || TO_CHAR(NOW(), 'YYYY') || '-'
                      || LPAD(NEXTVAL('advance_request_seq')::TEXT, 5, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_request_no
    BEFORE INSERT ON advances
    FOR EACH ROW
    WHEN (NEW.request_no IS NULL OR NEW.request_no = '')
    EXECUTE FUNCTION generate_request_no();
