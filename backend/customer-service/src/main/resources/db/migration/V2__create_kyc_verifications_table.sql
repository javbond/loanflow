-- V2__create_kyc_verifications_table.sql
-- KYC Verification tracking for e-KYC (US-029)

CREATE TABLE identity.kyc_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES identity.customers(id),
    aadhaar_number VARCHAR(12) NOT NULL,
    verification_type VARCHAR(20) NOT NULL DEFAULT 'AADHAAR_EKYC',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(100),
    otp_sent_at TIMESTAMP WITH TIME ZONE,
    verified_at TIMESTAMP WITH TIME ZONE,
    expired_at TIMESTAMP WITH TIME ZONE,
    ekyc_data JSONB,
    ckyc_number VARCHAR(20),
    ckyc_submitted_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(500),
    attempt_count INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ekyc_status CHECK (status IN ('PENDING', 'OTP_SENT', 'VERIFIED', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_verification_type CHECK (verification_type IN ('AADHAAR_EKYC', 'CKYC', 'MANUAL')),
    CONSTRAINT chk_kyc_aadhaar_format CHECK (aadhaar_number ~ '^[0-9]{12}$')
);

-- Indexes
CREATE INDEX idx_kyc_customer_id ON identity.kyc_verifications(customer_id);
CREATE INDEX idx_kyc_status ON identity.kyc_verifications(status);
CREATE INDEX idx_kyc_transaction_id ON identity.kyc_verifications(transaction_id);
CREATE INDEX idx_kyc_created_at ON identity.kyc_verifications(created_at DESC);

-- Update trigger (reuse function from V1)
CREATE TRIGGER update_kyc_verification_updated_at
    BEFORE UPDATE ON identity.kyc_verifications
    FOR EACH ROW
    EXECUTE FUNCTION identity.update_updated_at_column();

-- Comments
COMMENT ON TABLE identity.kyc_verifications IS 'UIDAI e-KYC verification records for customers';
COMMENT ON COLUMN identity.kyc_verifications.transaction_id IS 'UIDAI transaction reference for OTP flow';
COMMENT ON COLUMN identity.kyc_verifications.ekyc_data IS 'JSONB demographic data from UIDAI (name, DOB, gender, address, photo)';
COMMENT ON COLUMN identity.kyc_verifications.ckyc_number IS 'Central KYC Registry number after successful submission';
