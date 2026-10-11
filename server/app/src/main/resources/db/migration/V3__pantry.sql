-- M3: pantry items, created from food bill lines or added by hand (design-plan 6.2, ADR 0020).

CREATE TABLE pantry_items (
    id              UUID PRIMARY KEY,
    household_id    UUID        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    name            TEXT        NOT NULL,
    category        TEXT        NOT NULL CHECK (category IN (
        'PRODUCE', 'DAIRY', 'EGGS_MEAT', 'BAKERY', 'STAPLES', 'SNACKS', 'BEVERAGES', 'CONDIMENTS', 'FROZEN',
        'HOUSEHOLD', 'PERSONAL_CARE', 'OTHER')),
    quantity_amount BIGINT      NOT NULL CHECK (quantity_amount > 0),
    quantity_unit   TEXT        NOT NULL CHECK (quantity_unit IN ('MILLIGRAM', 'MILLILITRE', 'COUNT')),
    location_id     UUID REFERENCES locations (id) ON DELETE SET NULL,
    expires_on      DATE,
    status          TEXT        NOT NULL CHECK (status IN ('IN_STOCK', 'FINISHED', 'DISCARDED')),
    added_on        DATE        NOT NULL,
    price_paise     BIGINT CHECK (price_paise >= 0),
    payer_id        UUID REFERENCES users (id),
    source_line_id  UUID REFERENCES bill_lines (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX pantry_items_household_idx ON pantry_items (household_id, status);
CREATE UNIQUE INDEX pantry_items_source_line_idx ON pantry_items (source_line_id) WHERE source_line_id IS NOT NULL;

ALTER TABLE bill_lines ADD CONSTRAINT bill_lines_category_check CHECK (category IS NULL OR category IN (
    'PRODUCE', 'DAIRY', 'EGGS_MEAT', 'BAKERY', 'STAPLES', 'SNACKS', 'BEVERAGES', 'CONDIMENTS', 'FROZEN',
    'HOUSEHOLD', 'PERSONAL_CARE', 'OTHER'));
