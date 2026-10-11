-- M6: push tokens of members' phones, and which expiry alerts were already sent (ADR 0023).

CREATE TABLE device_tokens (
    token      TEXT PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    platform   TEXT        NOT NULL CHECK (platform IN ('ANDROID', 'IOS')),
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX device_tokens_user_idx ON device_tokens (user_id);

-- One row per item and kind of alert, so a sweep that runs again never repeats a notification.
CREATE TABLE expiry_alerts_sent (
    item_id UUID        NOT NULL REFERENCES pantry_items (id) ON DELETE CASCADE,
    kind    TEXT        NOT NULL CHECK (kind IN ('EXPIRES_SOON', 'EXPIRED')),
    sent_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (item_id, kind)
);
