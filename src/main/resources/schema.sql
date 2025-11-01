-- Schema definition for interest-calculator service

CREATE TABLE IF NOT EXISTS loan_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_holder_name VARCHAR(255) NOT NULL,
    interest_rate NUMERIC(9, 6) NOT NULL CHECK (interest_rate >= 0 AND interest_rate <= 100),
    interest_amount NUMERIC(18, 6) NOT NULL DEFAULT 0 CHECK (interest_amount >= 0),
    principal_amount NUMERIC(18, 6) NOT NULL CHECK (principal_amount >= 0.01),
    date_of_disbursal DATE NOT NULL,
    last_interest_applied_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (char_length(trim(account_holder_name)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_loan_accounts_last_interest_applied_at
    ON loan_accounts (last_interest_applied_at);
