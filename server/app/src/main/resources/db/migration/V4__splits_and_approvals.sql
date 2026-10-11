-- M4: per-line allocations, approvals and the submission time of a bill (design-plan 6.2 and 6.3, ADR 0021).

ALTER TABLE bills ADD COLUMN submitted_at TIMESTAMPTZ;
ALTER TABLE bills ADD COLUMN title TEXT;
ALTER TABLE bills ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
CREATE INDEX bills_pending_idx ON bills (submitted_at) WHERE status = 'PENDING';

CREATE TABLE allocations (
    line_id       UUID    NOT NULL REFERENCES bill_lines (id) ON DELETE CASCADE,
    user_id       UUID    NOT NULL REFERENCES users (id),
    mode          TEXT    NOT NULL CHECK (mode IN ('ONE', 'EQUAL', 'QUANTITY')),
    units         INTEGER NOT NULL CHECK (units > 0),
    amount_paise  BIGINT  NOT NULL CHECK (amount_paise >= 0),
    PRIMARY KEY (line_id, user_id)
);

CREATE TABLE approvals (
    bill_id     UUID        NOT NULL REFERENCES bills (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users (id),
    state       TEXT        NOT NULL CHECK (state IN ('PENDING', 'APPROVED', 'CHANGES_REQUESTED')),
    comment     TEXT,
    decided_at  TIMESTAMPTZ,
    automatic   BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (bill_id, user_id)
);
