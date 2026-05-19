-- advance-identity V2: журнал аудита запросов
CREATE TABLE audit_log (
    id          BIGSERIAL   PRIMARY KEY,
    request_id  UUID        NOT NULL,
    user_id     UUID,
    username    VARCHAR(100),
    role        VARCHAR(30),
    endpoint    VARCHAR(500) NOT NULL,
    http_method VARCHAR(10)  NOT NULL,
    status_code INTEGER,
    ip_address  INET,
    user_agent  TEXT,
    duration_ms INTEGER,
    created_at  TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX idx_audit_user_id    ON audit_log(user_id);
CREATE INDEX idx_audit_created_at ON audit_log(created_at);
