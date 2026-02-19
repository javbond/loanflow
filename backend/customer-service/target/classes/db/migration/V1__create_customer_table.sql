-- V1__create_customer_table.sql
-- Customer Schema for LoanFlow

CREATE SCHEMA IF NOT EXISTS identity;

-- Customers Table
CREATE TABLE identity.customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_number VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(10) NOT NULL,
    pan_number VARCHAR(10),
    aadhaar_number VARCHAR(12),
    aadhaar_verified BOOLEAN DEFAULT FALSE,
    pan_verified BOOLEAN DEFAULT FALSE,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    segment VARCHAR(20),

    -- Current Address
    current_address_line1 VARCHAR(200),
    current_address_line2 VARCHAR(200),
    current_landmark VARCHAR(100),
    current_city VARCHAR(100),
    current_state VARCHAR(100),
    current_pin_code VARCHAR(6),
    current_country VARCHAR(2) DEFAULT 'IN',
    current_ownership_type VARCHAR(20),
    current_years_at_address INTEGER,

    -- Permanent Address
    permanent_address_line1 VARCHAR(200),
    permanent_address_line2 VARCHAR(200),
    permanent_landmark VARCHAR(100),
    permanent_city VARCHAR(100),
    permanent_state VARCHAR(100),
    permanent_pin_code VARCHAR(6),
    permanent_country VARCHAR(2) DEFAULT 'IN',
    permanent_ownership_type VARCHAR(20),
    permanent_years_at_address INTEGER,

    -- Audit
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,

    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING_VERIFICATION')),
    CONSTRAINT chk_kyc_status CHECK (kyc_status IN ('PENDING', 'PARTIAL', 'VERIFIED', 'EXPIRED', 'REJECTED')),
    CONSTRAINT chk_pan_format CHECK (pan_number IS NULL OR pan_number ~ '^[A-Z]{5}[0-9]{4}[A-Z]$'),
    CONSTRAINT chk_aadhaar_format CHECK (aadhaar_number IS NULL OR aadhaar_number ~ '^[0-9]{12}$'),
    CONSTRAINT chk_mobile_format CHECK (mobile_number ~ '^[6-9][0-9]{9}$')
);

-- Indexes
CREATE INDEX idx_customer_number ON identity.customers(customer_number);
CREATE INDEX idx_customer_pan ON identity.customers(pan_number);
CREATE INDEX idx_customer_aadhaar ON identity.customers(aadhaar_number);
CREATE INDEX idx_customer_mobile ON identity.customers(mobile_number);
CREATE INDEX idx_customer_email ON identity.customers(email);
CREATE INDEX idx_customer_status ON identity.customers(status);
CREATE INDEX idx_customer_kyc ON identity.customers(kyc_status);
CREATE INDEX idx_customer_name ON identity.customers(first_name, last_name);

-- Unique constraints
CREATE UNIQUE INDEX idx_customer_pan_unique ON identity.customers(pan_number) WHERE pan_number IS NOT NULL;
CREATE UNIQUE INDEX idx_customer_aadhaar_unique ON identity.customers(aadhaar_number) WHERE aadhaar_number IS NOT NULL;
CREATE UNIQUE INDEX idx_customer_email_unique ON identity.customers(email);
CREATE UNIQUE INDEX idx_customer_mobile_unique ON identity.customers(mobile_number);

-- Update trigger
CREATE OR REPLACE FUNCTION identity.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_customer_updated_at
    BEFORE UPDATE ON identity.customers
    FOR EACH ROW
    EXECUTE FUNCTION identity.update_updated_at_column();

-- Comments
COMMENT ON TABLE identity.customers IS 'Customer master data with KYC information';
COMMENT ON COLUMN identity.customers.customer_number IS 'Unique customer identifier (CUS-YYYY-NNNNNN)';
COMMENT ON COLUMN identity.customers.kyc_status IS 'KYC verification status';
COMMENT ON COLUMN identity.customers.segment IS 'Customer segment (RETAIL, HNI, CORPORATE, SME)';
