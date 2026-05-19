-- advance-scoring V1: скоринг водителей и телематика

CREATE TABLE driver_scores (
    id                     UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id              UUID    UNIQUE NOT NULL,
    total_score            INTEGER NOT NULL DEFAULT 50 CHECK (total_score BETWEEN 0 AND 100),
    schedule_score         INTEGER DEFAULT 50,
    advance_closure_score  INTEGER DEFAULT 50,
    fuel_efficiency_score  INTEGER DEFAULT 50,
    default_history_score  INTEGER DEFAULT 50,
    tenure_score           INTEGER DEFAULT 50,
    trips_count            INTEGER DEFAULT 0,
    calculated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE score_history (
    id            BIGSERIAL   PRIMARY KEY,
    driver_id     UUID        NOT NULL,
    total_score   INTEGER,
    calculated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_score_history_driver ON score_history(driver_id, calculated_at DESC);

CREATE TABLE telematics_events (
    id            BIGSERIAL    PRIMARY KEY,
    driver_id     UUID         NOT NULL,
    vehicle_id    UUID,
    event_type    VARCHAR(50),
    latitude      NUMERIC(10,7),
    longitude     NUMERIC(10,7),
    speed_kmh     INTEGER,
    fuel_level_pct INTEGER,
    odometer_km   INTEGER,
    recorded_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_telematics_driver ON telematics_events(driver_id, recorded_at DESC);
