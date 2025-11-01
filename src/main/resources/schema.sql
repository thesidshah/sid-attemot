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

CREATE INDEX IF NOT EXISTS idx_loan_accounts_date_of_disbursal
    ON loan_accounts (date_of_disbursal);

COMMENT ON TABLE loan_accounts IS 'Stores loan account information with interest tracking';
COMMENT ON COLUMN loan_accounts.account_holder_name IS 'Name of the borrower associated with the loan';
COMMENT ON COLUMN loan_accounts.interest_rate IS 'Annual interest rate as percentage (e.g., 12.5 for 12.5%)';
COMMENT ON COLUMN loan_accounts.interest_amount IS 'Accumulated interest amount awaiting application to principal';
COMMENT ON COLUMN loan_accounts.principal_amount IS 'Current principal amount outstanding on the loan';
COMMENT ON COLUMN loan_accounts.last_interest_applied_at IS 'Timestamp of last interest application for idempotency';
COMMENT ON COLUMN loan_accounts.version IS 'Version field for optimistic locking';
