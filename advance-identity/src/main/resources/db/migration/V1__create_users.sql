-- advance-identity V1: таблица пользователей
CREATE TABLE users (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(36) UNIQUE NOT NULL,
    username    VARCHAR(100) UNIQUE NOT NULL,
    full_name   VARCHAR(200),
    email       VARCHAR(200),
    phone       VARCHAR(20),
    role        VARCHAR(30)  NOT NULL,
    active      BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_users_role        ON users(role);
