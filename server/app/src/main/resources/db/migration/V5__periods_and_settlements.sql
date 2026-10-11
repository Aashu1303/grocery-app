-- M5: settlement periods, the transfers that close them, and the period each bill belongs to (ADR 0022).

CREATE TABLE periods (
    id           UUID PRIMARY KEY,
    household_id UUID        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    month        TEXT        NOT NULL CHECK (month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    status       TEXT        NOT NULL CHECK (status IN ('OPEN', 'LOCKED')),
    created_at   TIMESTAMPTZ NOT NULL,
    closed_at    TIMESTAMPTZ,
    UNIQUE (household_id, month)
);
-- A household has at most one open period at a time.
CREATE UNIQUE INDEX periods_one_open_idx ON periods (household_id) WHERE status = 'OPEN';

CREATE TABLE settlements (
    id           UUID PRIMARY KEY,
    period_id    UUID   NOT NULL REFERENCES periods (id) ON DELETE CASCADE,
    from_user    UUID   NOT NULL REFERENCES users (id),
    to_user      UUID   NOT NULL REFERENCES users (id),
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    status       TEXT   NOT NULL CHECK (status IN ('PENDING', 'PAID')),
    paid_at      TIMESTAMPTZ,
    CHECK (from_user <> to_user)
);
CREATE INDEX settlements_period_idx ON settlements (period_id);

ALTER TABLE bills ADD COLUMN period_id UUID REFERENCES periods (id);
CREATE INDEX bills_period_idx ON bills (period_id);

-- Bills created before periods existed join one open period per household, labelled by the month of the migration.
INSERT INTO periods (id, household_id, month, status, created_at)
SELECT gen_random_uuid(), household_id, to_char(now(), 'YYYY-MM'), 'OPEN', now()
FROM (SELECT DISTINCT household_id FROM bills) AS households_with_bills;

UPDATE bills SET period_id = periods.id
FROM periods
WHERE periods.household_id = bills.household_id AND periods.status = 'OPEN';
