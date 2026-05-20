-- Fix UNIQUE constraint: code alone is not unique (same code exists per channel)
ALTER TABLE notification_templates
    DROP CONSTRAINT IF EXISTS notification_templates_code_key;

ALTER TABLE notification_templates
    ADD CONSTRAINT notification_templates_code_channel_key UNIQUE (code, channel);

-- Add read-tracking columns to notifications
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON notifications(recipient_id, is_read, created_at DESC);
