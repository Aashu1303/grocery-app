-- Firstclub (Trolleypop Retail) invoices get their own platform value.
ALTER TABLE bills DROP CONSTRAINT bills_platform_check;
ALTER TABLE bills ADD CONSTRAINT bills_platform_check
    CHECK (platform IN ('ZEPTO', 'BLINKIT', 'INSTAMART', 'HANDPICKD', 'FIRSTCLUB', 'MANUAL', 'OTHER'));
