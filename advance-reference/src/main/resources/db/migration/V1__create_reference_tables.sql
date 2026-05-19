-- advance-reference V1: справочные таблицы

CREATE TABLE contractors (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(300) NOT NULL,
    inn           VARCHAR(12)  UNIQUE,
    ogrn          VARCHAR(15),
    contact_phone VARCHAR(20),
    active        BOOLEAN      DEFAULT TRUE,
    created_at    TIMESTAMPTZ  DEFAULT NOW()
);

CREATE TABLE drivers (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID,
    full_name     VARCHAR(200) NOT NULL,
    license_no    VARCHAR(20)  UNIQUE,
    phone         VARCHAR(20),
    contractor_id UUID         REFERENCES contractors(id),
    active        BOOLEAN      DEFAULT TRUE,
    created_at    TIMESTAMPTZ  DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX idx_drivers_contractor ON drivers(contractor_id);
CREATE INDEX idx_drivers_user_id    ON drivers(user_id);

CREATE TABLE vehicles (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    plate_no            VARCHAR(20)  UNIQUE NOT NULL,
    model               VARCHAR(200),
    category            VARCHAR(50),
    fuel_norm_per_100km NUMERIC(6,2),
    tracker_id          VARCHAR(100),
    active              BOOLEAN      DEFAULT TRUE
);

CREATE TABLE routes (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(300) NOT NULL,
    origin           VARCHAR(200),
    destination      VARCHAR(200),
    distance_km      NUMERIC(8,2),
    estimated_hours  NUMERIC(5,1),
    active           BOOLEAN      DEFAULT TRUE,
    created_at       TIMESTAMPTZ  DEFAULT NOW()
);

CREATE TABLE fuel_norms (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_category VARCHAR(50) NOT NULL,
    base_norm        NUMERIC(6,2) NOT NULL,
    effective_from   DATE         NOT NULL,
    source_document  VARCHAR(100) DEFAULT 'АМ-23-р'
);

CREATE TABLE driver_limits (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id              UUID         NOT NULL REFERENCES drivers(id),
    advance_type           VARCHAR(30)  NOT NULL,
    monthly_limit          NUMERIC(12,2) NOT NULL,
    daily_limit            NUMERIC(10,2),
    auto_approve_threshold NUMERIC(10,2) DEFAULT 5000,
    UNIQUE(driver_id, advance_type)
);

CREATE INDEX idx_driver_limits_driver ON driver_limits(driver_id);
