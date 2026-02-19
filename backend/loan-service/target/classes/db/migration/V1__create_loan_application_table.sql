-- V1__create_loan_application_table.sql
-- Loan Application Schema for LoanFlow

CREATE SCHEMA IF NOT EXISTS application;

-- Loan Applications Table
CREATE TABLE application.loan_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id UUID NOT NULL,
    loan_type VARCHAR(30) NOT NULL,
    requested_amount DECIMAL(15, 2) NOT NULL,
    approved_amount DECIMAL(15, 2),
    interest_rate DECIMAL(5, 2),
    tenure_months INTEGER NOT NULL,
    emi_amount DECIMAL(15, 2),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    purpose VARCHAR(500),
    branch_code VARCHAR(10),
    assigned_officer UUID,
    cibil_score INTEGER,
    risk_category VARCHAR(20),
    processing_fee DECIMAL(15, 2),
    rejection_reason VARCHAR(500),
    workflow_instance_id VARCHAR(50),
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    disbursed_at TIMESTAMP WITH TIME ZONE,
    expected_disbursement_date DATE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,

    CONSTRAINT chk_loan_type CHECK (loan_type IN ('HOME_LOAN', 'PERSONAL_LOAN', 'VEHICLE_LOAN', 'BUSINESS_LOAN', 'EDUCATION_LOAN', 'GOLD_LOAN', 'LAP')),
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'DOCUMENT_VERIFICATION', 'CREDIT_CHECK', 'UNDERWRITING', 'CONDITIONALLY_APPROVED', 'REFERRED', 'APPROVED', 'DISBURSEMENT_PENDING', 'DISBURSED', 'RETURNED', 'REJECTED', 'CANCELLED', 'CLOSED', 'NPA')),
    CONSTRAINT chk_requested_amount CHECK (requested_amount >= 10000 AND requested_amount <= 100000000),
    CONSTRAINT chk_tenure CHECK (tenure_months >= 6 AND tenure_months <= 360),
    CONSTRAINT chk_cibil_score CHECK (cibil_score IS NULL OR (cibil_score >= 300 AND cibil_score <= 900))
);

-- Indexes
CREATE INDEX idx_loan_app_customer ON application.loan_applications(customer_id);
CREATE INDEX idx_loan_app_status ON application.loan_applications(status);
CREATE INDEX idx_loan_app_branch ON application.loan_applications(branch_code);
CREATE INDEX idx_loan_app_officer ON application.loan_applications(assigned_officer);
CREATE INDEX idx_loan_app_created ON application.loan_applications(created_at DESC);
CREATE INDEX idx_loan_app_submitted ON application.loan_applications(submitted_at DESC);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION application.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for auto-updating updated_at
CREATE TRIGGER update_loan_application_updated_at
    BEFORE UPDATE ON application.loan_applications
    FOR EACH ROW
    EXECUTE FUNCTION application.update_updated_at_column();

-- Comments
COMMENT ON TABLE application.loan_applications IS 'Main table for loan applications';
COMMENT ON COLUMN application.loan_applications.application_number IS 'Unique human-readable application identifier (LN-YYYY-NNNNNN)';
COMMENT ON COLUMN application.loan_applications.status IS 'Current workflow status of the application';
COMMENT ON COLUMN application.loan_applications.cibil_score IS 'Credit score from CIBIL bureau (300-900)';
COMMENT ON COLUMN application.loan_applications.workflow_instance_id IS 'Flowable BPMN process instance ID';
