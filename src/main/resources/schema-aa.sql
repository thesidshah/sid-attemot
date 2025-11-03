-- Schema definition for Account Aggregator integration

-- Consent requests table
CREATE TABLE IF NOT EXISTS consent_requests (
    id BIGSERIAL PRIMARY KEY,
    loan_account_id BIGINT NOT NULL REFERENCES loan_accounts(id) ON DELETE CASCADE,
    consent_request_id VARCHAR(255) UNIQUE,
    gateway_token_id VARCHAR(255),
    customer_ref_id VARCHAR(255) NOT NULL UNIQUE,
    template_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    redirect_url VARCHAR(500),
    consent_start_date TIMESTAMPTZ,
    consent_expiry_date TIMESTAMPTZ,
    fi_start_date DATE,
    fi_end_date DATE,
    fi_request_id VARCHAR(255),
    fi_request_status VARCHAR(50),
    fi_requested_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    customer_message VARCHAR(500),
    customer_notification_mode VARCHAR(50),
    notify_customer BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'DATA_FETCHED')),
    CHECK (customer_notification_mode IN ('WHATSAPP', 'SMS') OR customer_notification_mode IS NULL)
);

-- Financial data table
CREATE TABLE IF NOT EXISTS financial_data (
    id BIGSERIAL PRIMARY KEY,
    consent_request_id BIGINT NOT NULL UNIQUE REFERENCES consent_requests(id) ON DELETE CASCADE,
    fi_request_id VARCHAR(255) UNIQUE,
    fi_status VARCHAR(50),
    raw_response TEXT,
    fip_count INTEGER,
    account_count INTEGER,
    data_fetched_at TIMESTAMPTZ,
    data_expires_at TIMESTAMPTZ NOT NULL,
    is_purged BOOLEAN NOT NULL DEFAULT false,
    purged_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (fip_count >= 0),
    CHECK (account_count >= 0)
);

-- FIP accounts table
CREATE TABLE IF NOT EXISTS fip_accounts (
    id BIGSERIAL PRIMARY KEY,
    financial_data_id BIGINT NOT NULL REFERENCES financial_data(id) ON DELETE CASCADE,
    fip_id VARCHAR(100) NOT NULL,
    fi_type VARCHAR(100) NOT NULL,
    account_number VARCHAR(100),
    account_id VARCHAR(255),
    fi_data_id VARCHAR(255),
    account_analytics_available BOOLEAN,
    account_sub_analytics_available BOOLEAN,
    fi_data TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for consent_requests
CREATE INDEX IF NOT EXISTS idx_consent_request_id
    ON consent_requests(consent_request_id);

CREATE INDEX IF NOT EXISTS idx_customer_ref_id
    ON consent_requests(customer_ref_id);

CREATE INDEX IF NOT EXISTS idx_loan_account_id
    ON consent_requests(loan_account_id);

CREATE INDEX IF NOT EXISTS idx_consent_status
    ON consent_requests(status);

CREATE INDEX IF NOT EXISTS idx_consent_fi_request_id
    ON consent_requests(fi_request_id);

CREATE INDEX IF NOT EXISTS idx_consent_created_at
    ON consent_requests(created_at DESC);

-- Indexes for financial_data
CREATE INDEX IF NOT EXISTS idx_financial_data_fi_request_id
    ON financial_data(fi_request_id);

CREATE INDEX IF NOT EXISTS idx_financial_data_expiry
    ON financial_data(data_expires_at) WHERE is_purged = false;

CREATE INDEX IF NOT EXISTS idx_financial_data_consent
    ON financial_data(consent_request_id);

-- Indexes for fip_accounts
CREATE INDEX IF NOT EXISTS idx_fip_accounts_financial_data
    ON fip_accounts(financial_data_id);

CREATE INDEX IF NOT EXISTS idx_fip_accounts_fip_id
    ON fip_accounts(fip_id);

CREATE INDEX IF NOT EXISTS idx_fip_accounts_fi_type
    ON fip_accounts(fi_type);

-- Comments for documentation
COMMENT ON TABLE consent_requests IS 'Stores consent requests for Account Aggregator integration';
COMMENT ON COLUMN consent_requests.consent_request_id IS 'Unique consent request ID from Digio AA gateway';
COMMENT ON COLUMN consent_requests.gateway_token_id IS 'Token ID used for redirect URL to AA gateway';
COMMENT ON COLUMN consent_requests.customer_ref_id IS 'Unique reference ID for the customer in this consent request';
COMMENT ON COLUMN consent_requests.template_id IS 'Template ID used for the consent request';
COMMENT ON COLUMN consent_requests.status IS 'Status: PENDING, APPROVED, REJECTED, EXPIRED, DATA_FETCHED';
COMMENT ON COLUMN consent_requests.redirect_url IS 'URL where customer completes consent approval';
COMMENT ON COLUMN consent_requests.fi_request_id IS 'Financial information request ID after consent approval';
COMMENT ON COLUMN consent_requests.fi_request_status IS 'Status of the FI request';
COMMENT ON COLUMN consent_requests.version IS 'Version field for optimistic locking';

COMMENT ON TABLE financial_data IS 'Stores fetched financial data from Account Aggregator';
COMMENT ON COLUMN financial_data.fi_request_id IS 'Financial information request ID';
COMMENT ON COLUMN financial_data.fi_status IS 'Status of the FI data fetch';
COMMENT ON COLUMN financial_data.raw_response IS 'Raw JSON response from AA gateway';
COMMENT ON COLUMN financial_data.fip_count IS 'Number of Financial Information Providers';
COMMENT ON COLUMN financial_data.account_count IS 'Total number of accounts fetched';
COMMENT ON COLUMN financial_data.data_expires_at IS 'When this financial data expires and should be purged';
COMMENT ON COLUMN financial_data.is_purged IS 'Whether the sensitive data has been purged';
COMMENT ON COLUMN financial_data.version IS 'Version field for optimistic locking';

COMMENT ON TABLE fip_accounts IS 'Stores individual FIP account details';
COMMENT ON COLUMN fip_accounts.fip_id IS 'Financial Information Provider ID';
COMMENT ON COLUMN fip_accounts.fi_type IS 'Type of financial information (e.g., DEPOSIT, TERM_DEPOSIT)';
COMMENT ON COLUMN fip_accounts.account_number IS 'Masked or full account number';
COMMENT ON COLUMN fip_accounts.fi_data IS 'JSON data for this specific account';
