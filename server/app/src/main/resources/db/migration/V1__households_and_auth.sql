-- M1: users, households, roles, invites, locations, sessions and idempotency (design-plan 6.2, ADR 0015).

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    firebase_uid  TEXT        NOT NULL UNIQUE,
    phone         TEXT        NOT NULL UNIQUE,
    display_name  TEXT        NOT NULL,
    upi_vpa       TEXT,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE households (
    id          UUID PRIMARY KEY,
    name        TEXT        NOT NULL,
    time_zone   TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE memberships (
    household_id  UUID        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role          TEXT        NOT NULL CHECK (role IN ('OWNER', 'MEMBER', 'COOK')),
    joined_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (household_id, user_id)
);
CREATE INDEX memberships_user_idx ON memberships (user_id);

CREATE TABLE invites (
    code          TEXT PRIMARY KEY,
    household_id  UUID        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    role          TEXT        NOT NULL CHECK (role IN ('OWNER', 'MEMBER', 'COOK')),
    created_by    UUID        NOT NULL REFERENCES users (id),
    expires_at    TIMESTAMPTZ NOT NULL,
    accepted_by   UUID REFERENCES users (id),
    accepted_at   TIMESTAMPTZ
);

CREATE TABLE locations (
    id            UUID PRIMARY KEY,
    household_id  UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    kind          TEXT NOT NULL CHECK (kind IN ('FRIDGE', 'FREEZER', 'SHELF', 'OTHER')),
    UNIQUE (household_id, name)
);

CREATE TABLE sessions (
    token_hash  TEXT PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX sessions_user_idx ON sessions (user_id);

CREATE TABLE idempotency_keys (
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    key            TEXT        NOT NULL,
    request_hash   TEXT        NOT NULL,
    status_code    INT         NOT NULL,
    response_body  TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, key)
);
