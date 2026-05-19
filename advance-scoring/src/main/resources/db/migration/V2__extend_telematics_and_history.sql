-- Add DTO-mapped columns to telematics_events
ALTER TABLE telematics_events
    ADD COLUMN IF NOT EXISTS route_id            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS occurred_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS distance_km         NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS fuel_consumed_liters NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS speeding_minutes    INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS harsh_braking_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS on_schedule         BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS trip_stage          VARCHAR(50);

-- Add component scores to score_history
ALTER TABLE score_history
    ADD COLUMN IF NOT EXISTS schedule_score         INTEGER,
    ADD COLUMN IF NOT EXISTS advance_closure_score  INTEGER,
    ADD COLUMN IF NOT EXISTS fuel_efficiency_score  INTEGER,
    ADD COLUMN IF NOT EXISTS default_history_score  INTEGER,
    ADD COLUMN IF NOT EXISTS tenure_score           INTEGER;

-- Add version column to driver_scores for optimistic locking
ALTER TABLE driver_scores
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
