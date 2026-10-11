-- M2: imported bills, their source PDFs, lines and charges (design-plan 6.2, ADR 0018).

CREATE TABLE bills (
    id            UUID PRIMARY KEY,
    household_id  UUID        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    platform      TEXT        NOT NULL CHECK (platform IN ('ZEPTO', 'BLINKIT', 'INSTAMART', 'HANDPICKD', 'MANUAL', 'OTHER')),
    invoice_no    TEXT        NOT NULL,
    order_id      TEXT,
    invoice_date  DATE        NOT NULL,
    payer_id      UUID        NOT NULL REFERENCES users (id),
    created_by    UUID        NOT NULL REFERENCES users (id),
    status        TEXT        NOT NULL CHECK (status IN ('DRAFT', 'PENDING', 'CHANGES_REQUESTED', 'APPROVED', 'LOCKED')),
    confidence    TEXT        NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    total_paise   BIGINT      NOT NULL CHECK (total_paise >= 0),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    UNIQUE (household_id, platform, invoice_no)
);
CREATE INDEX bills_household_idx ON bills (household_id);

CREATE TABLE bill_documents (
    id            UUID PRIMARY KEY,
    bill_id       UUID        NOT NULL REFERENCES bills (id) ON DELETE CASCADE,
    household_id  UUID        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    platform      TEXT        NOT NULL,
    invoice_no    TEXT        NOT NULL,
    storage_key   TEXT        NOT NULL,
    sha256        TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    UNIQUE (household_id, sha256),
    UNIQUE (household_id, platform, invoice_no)
);

CREATE TABLE bill_lines (
    id             UUID PRIMARY KEY,
    bill_id        UUID    NOT NULL REFERENCES bills (id) ON DELETE CASCADE,
    position       INTEGER NOT NULL,
    raw_name       TEXT    NOT NULL,
    display_name   TEXT    NOT NULL,
    units          INTEGER NOT NULL CHECK (units > 0),
    amount_paise   BIGINT  NOT NULL CHECK (amount_paise >= 0),
    measure_amount BIGINT CHECK (measure_amount >= 0),
    measure_unit   TEXT CHECK (measure_unit IN ('MILLIGRAM', 'MILLILITRE', 'COUNT')),
    mrp_paise      BIGINT,
    hsn            TEXT,
    upc            TEXT,
    category       TEXT,
    is_food        BOOLEAN,
    CHECK ((measure_amount IS NULL) = (measure_unit IS NULL))
);
CREATE INDEX bill_lines_bill_idx ON bill_lines (bill_id, position);

CREATE TABLE bill_charges (
    id            UUID PRIMARY KEY,
    bill_id       UUID    NOT NULL REFERENCES bills (id) ON DELETE CASCADE,
    position      INTEGER NOT NULL,
    kind          TEXT    NOT NULL CHECK (kind IN ('HANDLING', 'DELIVERY', 'DISCOUNT', 'ROUND_OFF', 'OTHER')),
    label         TEXT    NOT NULL,
    amount_paise  BIGINT  NOT NULL,
    distribution  TEXT    NOT NULL DEFAULT 'PROPORTIONAL' CHECK (distribution IN ('PROPORTIONAL', 'EQUAL'))
);
CREATE INDEX bill_charges_bill_idx ON bill_charges (bill_id, position);
