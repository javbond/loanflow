-- =============================================================================
-- LOANFLOW - LOAN ORIGINATION SYSTEM
-- PostgreSQL Database Schema
-- Version: 1.0.0
-- Created: 2025-02-16
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- SCHEMA: IDENTITY
-- Purpose: User management, authentication, roles, and permissions
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS identity;

-- -----------------------------------------------------------------------------
-- Table: identity.users
-- Purpose: Store user account information
-- -----------------------------------------------------------------------------
CREATE TABLE identity.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    employee_id VARCHAR(50) UNIQUE,
    email VARCHAR(255) UNIQUE NOT NULL,
    mobile VARCHAR(15),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    display_name VARCHAR(200),
    branch_code VARCHAR(20),
    department VARCHAR(100),
    designation VARCHAR(100),
    reporting_to UUID REFERENCES identity.users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED')),
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id),
    updated_by UUID REFERENCES identity.users(id)
);

CREATE INDEX idx_users_email ON identity.users(email);
CREATE INDEX idx_users_employee_id ON identity.users(employee_id);
CREATE INDEX idx_users_branch_code ON identity.users(branch_code);
CREATE INDEX idx_users_status ON identity.users(status);

-- -----------------------------------------------------------------------------
-- Table: identity.roles
-- Purpose: Define system roles
-- -----------------------------------------------------------------------------
CREATE TABLE identity.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id),
    updated_by UUID REFERENCES identity.users(id)
);

-- Insert default roles
INSERT INTO identity.roles (code, name, description, is_system_role) VALUES
    ('SUPER_ADMIN', 'Super Administrator', 'Full system access', TRUE),
    ('ADMIN', 'Administrator', 'Administrative access', TRUE),
    ('LOAN_OFFICER', 'Loan Officer', 'Branch loan officer for application capture', TRUE),
    ('CREDIT_ANALYST', 'Credit Analyst', 'Credit assessment and analysis', TRUE),
    ('UNDERWRITER', 'Underwriter', 'Loan underwriting and decision making', TRUE),
    ('SENIOR_UNDERWRITER', 'Senior Underwriter', 'Senior underwriting with higher limits', TRUE),
    ('BRANCH_MANAGER', 'Branch Manager', 'Branch level approvals', TRUE),
    ('REGIONAL_MANAGER', 'Regional Manager', 'Regional level approvals', TRUE),
    ('COMPLIANCE_OFFICER', 'Compliance Officer', 'Regulatory compliance monitoring', TRUE),
    ('PRODUCT_MANAGER', 'Product Manager', 'Loan product configuration', TRUE),
    ('CUSTOMER', 'Customer', 'Self-service customer access', TRUE);

-- -----------------------------------------------------------------------------
-- Table: identity.permissions
-- Purpose: Define granular permissions
-- -----------------------------------------------------------------------------
CREATE TABLE identity.permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert core permissions
INSERT INTO identity.permissions (code, name, module, action, resource) VALUES
    -- Application permissions
    ('APPLICATION_CREATE', 'Create Loan Application', 'APPLICATION', 'CREATE', 'loan_application'),
    ('APPLICATION_VIEW', 'View Loan Application', 'APPLICATION', 'VIEW', 'loan_application'),
    ('APPLICATION_UPDATE', 'Update Loan Application', 'APPLICATION', 'UPDATE', 'loan_application'),
    ('APPLICATION_DELETE', 'Delete Loan Application', 'APPLICATION', 'DELETE', 'loan_application'),
    ('APPLICATION_SUBMIT', 'Submit Loan Application', 'APPLICATION', 'SUBMIT', 'loan_application'),
    -- Underwriting permissions
    ('UNDERWRITING_VIEW', 'View Underwriting Queue', 'UNDERWRITING', 'VIEW', 'underwriting_task'),
    ('UNDERWRITING_DECIDE', 'Make Underwriting Decision', 'UNDERWRITING', 'DECIDE', 'underwriting_task'),
    ('UNDERWRITING_APPROVE', 'Approve Loan', 'UNDERWRITING', 'APPROVE', 'loan_application'),
    ('UNDERWRITING_REJECT', 'Reject Loan', 'UNDERWRITING', 'REJECT', 'loan_application'),
    -- Policy permissions
    ('POLICY_CREATE', 'Create Policy', 'POLICY', 'CREATE', 'policy'),
    ('POLICY_VIEW', 'View Policy', 'POLICY', 'VIEW', 'policy'),
    ('POLICY_UPDATE', 'Update Policy', 'POLICY', 'UPDATE', 'policy'),
    ('POLICY_ACTIVATE', 'Activate Policy', 'POLICY', 'ACTIVATE', 'policy'),
    ('POLICY_DEACTIVATE', 'Deactivate Policy', 'POLICY', 'DEACTIVATE', 'policy'),
    -- Document permissions
    ('DOCUMENT_UPLOAD', 'Upload Document', 'DOCUMENT', 'UPLOAD', 'document'),
    ('DOCUMENT_VIEW', 'View Document', 'DOCUMENT', 'VIEW', 'document'),
    ('DOCUMENT_VERIFY', 'Verify Document', 'DOCUMENT', 'VERIFY', 'document'),
    ('DOCUMENT_REJECT', 'Reject Document', 'DOCUMENT', 'REJECT', 'document'),
    -- Compliance permissions
    ('COMPLIANCE_VIEW', 'View Compliance Records', 'COMPLIANCE', 'VIEW', 'compliance_record'),
    ('COMPLIANCE_AUDIT', 'Access Audit Logs', 'COMPLIANCE', 'AUDIT', 'audit_log'),
    ('COMPLIANCE_REPORT', 'Generate Compliance Reports', 'COMPLIANCE', 'REPORT', 'compliance_report'),
    -- Admin permissions
    ('USER_MANAGE', 'Manage Users', 'ADMIN', 'MANAGE', 'user'),
    ('ROLE_MANAGE', 'Manage Roles', 'ADMIN', 'MANAGE', 'role'),
    ('SYSTEM_CONFIG', 'System Configuration', 'ADMIN', 'CONFIG', 'system');

-- -----------------------------------------------------------------------------
-- Table: identity.role_permissions
-- Purpose: Map roles to permissions (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE identity.role_permissions (
    role_id UUID NOT NULL REFERENCES identity.roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES identity.permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id),
    PRIMARY KEY (role_id, permission_id)
);

-- -----------------------------------------------------------------------------
-- Table: identity.user_roles
-- Purpose: Assign roles to users (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE identity.user_roles (
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES identity.roles(id) ON DELETE CASCADE,
    branch_code VARCHAR(20),
    valid_from DATE DEFAULT CURRENT_DATE,
    valid_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON identity.user_roles(user_id);
CREATE INDEX idx_user_roles_role ON identity.user_roles(role_id);

-- =============================================================================
-- SCHEMA: APPLICATION
-- Purpose: Loan application management - Core domain
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS application;

-- -----------------------------------------------------------------------------
-- Table: application.loan_products
-- Purpose: Define loan product types and configurations
-- -----------------------------------------------------------------------------
CREATE TABLE application.loan_products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('RETAIL', 'CORPORATE', 'PRIORITY_SECTOR', 'AGRICULTURE')),
    sub_category VARCHAR(50),
    description TEXT,
    min_amount DECIMAL(18, 2) NOT NULL,
    max_amount DECIMAL(18, 2) NOT NULL,
    min_tenure_months INTEGER NOT NULL,
    max_tenure_months INTEGER NOT NULL,
    min_interest_rate DECIMAL(5, 2) NOT NULL,
    max_interest_rate DECIMAL(5, 2) NOT NULL,
    processing_fee_percent DECIMAL(5, 2) DEFAULT 0,
    processing_fee_min DECIMAL(18, 2) DEFAULT 0,
    processing_fee_max DECIMAL(18, 2),
    is_secured BOOLEAN DEFAULT FALSE,
    collateral_required BOOLEAN DEFAULT FALSE,
    psl_eligible BOOLEAN DEFAULT FALSE,
    psl_category VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id),
    updated_by UUID REFERENCES identity.users(id)
);

-- Insert default loan products
INSERT INTO application.loan_products (code, name, category, min_amount, max_amount, min_tenure_months, max_tenure_months, min_interest_rate, max_interest_rate, is_secured, collateral_required, psl_eligible) VALUES
    ('PL', 'Personal Loan', 'RETAIL', 50000, 5000000, 12, 60, 10.50, 24.00, FALSE, FALSE, FALSE),
    ('HL', 'Home Loan', 'RETAIL', 500000, 100000000, 60, 360, 8.50, 12.00, TRUE, TRUE, FALSE),
    ('VL', 'Vehicle Loan', 'RETAIL', 100000, 10000000, 12, 84, 9.00, 15.00, TRUE, TRUE, FALSE),
    ('GL', 'Gold Loan', 'RETAIL', 10000, 5000000, 3, 36, 7.00, 12.00, TRUE, TRUE, TRUE),
    ('MSME_WC', 'MSME Working Capital', 'CORPORATE', 100000, 50000000, 12, 60, 10.00, 16.00, TRUE, TRUE, TRUE),
    ('KCC', 'Kisan Credit Card', 'AGRICULTURE', 10000, 300000, 12, 60, 4.00, 9.00, FALSE, FALSE, TRUE);

-- -----------------------------------------------------------------------------
-- Table: application.loan_applications
-- Purpose: Main loan application entity (Aggregate Root)
-- -----------------------------------------------------------------------------
CREATE TABLE application.loan_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_number VARCHAR(20) UNIQUE NOT NULL,
    product_id UUID NOT NULL REFERENCES application.loan_products(id),
    branch_code VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'BRANCH' CHECK (channel IN ('BRANCH', 'ONLINE', 'MOBILE', 'DSA', 'API')),

    -- Loan Details
    requested_amount DECIMAL(18, 2) NOT NULL,
    approved_amount DECIMAL(18, 2),
    tenure_months INTEGER NOT NULL,
    purpose VARCHAR(500),

    -- Status Tracking
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'DOCUMENTS_PENDING', 'UNDER_VERIFICATION',
        'CREDIT_CHECK', 'POLICY_EVALUATION', 'UNDER_REVIEW', 'PENDING_APPROVAL',
        'APPROVED', 'CONDITIONALLY_APPROVED', 'REJECTED', 'WITHDRAWN',
        'DISBURSEMENT_PENDING', 'PARTIALLY_DISBURSED', 'DISBURSED', 'CANCELLED'
    )),
    sub_status VARCHAR(50),

    -- Decision Details
    decision VARCHAR(20) CHECK (decision IN ('APPROVED', 'REJECTED', 'REFERRED')),
    decision_date TIMESTAMP WITH TIME ZONE,
    decision_by UUID REFERENCES identity.users(id),
    decision_remarks TEXT,
    rejection_reasons TEXT[],

    -- Scoring
    internal_score DECIMAL(5, 2),
    risk_grade VARCHAR(10),

    -- Timestamps
    submitted_at TIMESTAMP WITH TIME ZONE,
    expected_disbursement_date DATE,
    actual_disbursement_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id),
    updated_by UUID REFERENCES identity.users(id),

    -- Soft Delete
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES identity.users(id)
);

CREATE INDEX idx_loan_applications_number ON application.loan_applications(application_number);
CREATE INDEX idx_loan_applications_status ON application.loan_applications(status);
CREATE INDEX idx_loan_applications_branch ON application.loan_applications(branch_code);
CREATE INDEX idx_loan_applications_product ON application.loan_applications(product_id);
CREATE INDEX idx_loan_applications_created_at ON application.loan_applications(created_at);

-- Generate application number sequence
CREATE SEQUENCE application.application_number_seq START WITH 100001;

-- Function to generate application number
CREATE OR REPLACE FUNCTION application.generate_application_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.application_number := 'LF' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || LPAD(NEXTVAL('application.application_number_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_application_number
    BEFORE INSERT ON application.loan_applications
    FOR EACH ROW
    WHEN (NEW.application_number IS NULL)
    EXECUTE FUNCTION application.generate_application_number();

-- -----------------------------------------------------------------------------
-- Table: application.applicants
-- Purpose: Store applicant information (primary and co-applicants)
-- -----------------------------------------------------------------------------
CREATE TABLE application.applicants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,
    applicant_type VARCHAR(20) NOT NULL DEFAULT 'PRIMARY' CHECK (applicant_type IN ('PRIMARY', 'CO_APPLICANT', 'GUARANTOR')),
    relationship_with_primary VARCHAR(50),

    -- Identity
    customer_id VARCHAR(50),
    salutation VARCHAR(10),
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    father_name VARCHAR(200),
    mother_name VARCHAR(200),
    spouse_name VARCHAR(200),

    -- Demographics
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    marital_status VARCHAR(20) CHECK (marital_status IN ('SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED')),
    nationality VARCHAR(50) DEFAULT 'INDIAN',
    resident_status VARCHAR(20) DEFAULT 'RESIDENT' CHECK (resident_status IN ('RESIDENT', 'NRI', 'PIO', 'FOREIGN_NATIONAL')),

    -- KYC Documents (stored masked/tokenized)
    pan VARCHAR(10) NOT NULL,
    pan_verified BOOLEAN DEFAULT FALSE,
    aadhaar_last4 VARCHAR(4),
    aadhaar_verified BOOLEAN DEFAULT FALSE,
    voter_id VARCHAR(20),
    passport_number VARCHAR(20),
    driving_license VARCHAR(30),

    -- Contact
    mobile_primary VARCHAR(15) NOT NULL,
    mobile_secondary VARCHAR(15),
    email VARCHAR(255),

    -- Financial
    annual_income DECIMAL(18, 2),
    monthly_income DECIMAL(18, 2),
    income_source VARCHAR(50),
    existing_emi DECIMAL(18, 2) DEFAULT 0,

    -- Metadata
    is_politically_exposed BOOLEAN DEFAULT FALSE,
    kyc_verified BOOLEAN DEFAULT FALSE,
    kyc_verified_at TIMESTAMP WITH TIME ZONE,
    ckyc_number VARCHAR(20),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_applicants_application ON application.applicants(application_id);
CREATE INDEX idx_applicants_pan ON application.applicants(pan);
CREATE INDEX idx_applicants_mobile ON application.applicants(mobile_primary);

-- -----------------------------------------------------------------------------
-- Table: application.addresses
-- Purpose: Store applicant addresses (residential, office, permanent)
-- -----------------------------------------------------------------------------
CREATE TABLE application.addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    applicant_id UUID NOT NULL REFERENCES application.applicants(id) ON DELETE CASCADE,
    address_type VARCHAR(20) NOT NULL CHECK (address_type IN ('CURRENT', 'PERMANENT', 'OFFICE', 'COMMUNICATION')),

    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    address_line3 VARCHAR(255),
    landmark VARCHAR(200),
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    country VARCHAR(50) DEFAULT 'INDIA',

    residence_type VARCHAR(30) CHECK (residence_type IN ('OWNED', 'RENTED', 'PARENTAL', 'COMPANY_PROVIDED', 'PG')),
    years_at_address INTEGER,

    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES identity.users(id),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_addresses_applicant ON application.addresses(applicant_id);

-- -----------------------------------------------------------------------------
-- Table: application.employment_details
-- Purpose: Store employment/business information
-- -----------------------------------------------------------------------------
CREATE TABLE application.employment_details (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    applicant_id UUID NOT NULL REFERENCES application.applicants(id) ON DELETE CASCADE,

    employment_type VARCHAR(30) NOT NULL CHECK (employment_type IN (
        'SALARIED', 'SELF_EMPLOYED_PROFESSIONAL', 'SELF_EMPLOYED_BUSINESS',
        'AGRICULTURIST', 'RETIRED', 'HOMEMAKER', 'STUDENT', 'UNEMPLOYED'
    )),

    -- For Salaried
    employer_name VARCHAR(200),
    employer_type VARCHAR(30) CHECK (employer_type IN ('GOVERNMENT', 'PSU', 'MNC', 'PRIVATE_LIMITED', 'PARTNERSHIP', 'PROPRIETORSHIP', 'OTHERS')),
    designation VARCHAR(100),
    department VARCHAR(100),
    employee_id VARCHAR(50),
    date_of_joining DATE,
    years_in_current_job INTEGER,
    total_experience_years INTEGER,
    office_email VARCHAR(255),
    office_phone VARCHAR(20),

    -- For Self-Employed
    business_name VARCHAR(200),
    business_type VARCHAR(50),
    constitution VARCHAR(30) CHECK (constitution IN ('PROPRIETORSHIP', 'PARTNERSHIP', 'PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'LLP', 'HUF', 'TRUST')),
    gstin VARCHAR(20),
    business_vintage_years INTEGER,
    annual_turnover DECIMAL(18, 2),
    net_profit DECIMAL(18, 2),

    -- Income Details
    gross_monthly_income DECIMAL(18, 2),
    net_monthly_income DECIMAL(18, 2),
    other_income DECIMAL(18, 2) DEFAULT 0,
    other_income_source VARCHAR(200),

    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employment_applicant ON application.employment_details(applicant_id);

-- -----------------------------------------------------------------------------
-- Table: application.collaterals
-- Purpose: Store collateral/security details for secured loans
-- -----------------------------------------------------------------------------
CREATE TABLE application.collaterals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,

    collateral_type VARCHAR(30) NOT NULL CHECK (collateral_type IN (
        'PROPERTY_RESIDENTIAL', 'PROPERTY_COMMERCIAL', 'PROPERTY_INDUSTRIAL', 'PROPERTY_LAND',
        'VEHICLE_NEW', 'VEHICLE_USED', 'GOLD', 'FD', 'SHARES', 'MACHINERY', 'INVENTORY', 'RECEIVABLES', 'OTHER'
    )),

    -- Property Details
    property_address TEXT,
    property_city VARCHAR(100),
    property_state VARCHAR(100),
    property_pincode VARCHAR(10),
    property_area_sqft DECIMAL(12, 2),
    property_age_years INTEGER,
    property_ownership VARCHAR(30) CHECK (property_ownership IN ('SELF', 'SPOUSE', 'PARENTS', 'JOINT', 'THIRD_PARTY')),

    -- Vehicle Details
    vehicle_make VARCHAR(100),
    vehicle_model VARCHAR(100),
    vehicle_variant VARCHAR(100),
    vehicle_year INTEGER,
    vehicle_registration VARCHAR(20),

    -- Gold Details
    gold_weight_grams DECIMAL(10, 3),
    gold_purity VARCHAR(10),
    gold_type VARCHAR(30),

    -- Valuation
    market_value DECIMAL(18, 2),
    forced_sale_value DECIMAL(18, 2),
    valuation_date DATE,
    valuation_agency VARCHAR(200),
    valuation_report_number VARCHAR(50),

    -- Legal
    is_encumbered BOOLEAN DEFAULT FALSE,
    existing_charges TEXT,
    cersai_registration_number VARCHAR(50),
    cersai_registration_date DATE,

    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED', 'RELEASED')),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_collaterals_application ON application.collaterals(application_id);

-- -----------------------------------------------------------------------------
-- Table: application.loan_offers
-- Purpose: Store loan offers generated for applications
-- -----------------------------------------------------------------------------
CREATE TABLE application.loan_offers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,
    offer_number VARCHAR(30) UNIQUE NOT NULL,

    -- Offer Details
    offered_amount DECIMAL(18, 2) NOT NULL,
    interest_rate DECIMAL(5, 2) NOT NULL,
    interest_type VARCHAR(20) DEFAULT 'REDUCING' CHECK (interest_type IN ('REDUCING', 'FLAT')),
    tenure_months INTEGER NOT NULL,
    emi DECIMAL(18, 2) NOT NULL,

    -- Fees
    processing_fee DECIMAL(18, 2) DEFAULT 0,
    processing_fee_waived DECIMAL(18, 2) DEFAULT 0,
    documentation_charges DECIMAL(18, 2) DEFAULT 0,
    insurance_premium DECIMAL(18, 2) DEFAULT 0,

    -- Net Disbursement
    total_deductions DECIMAL(18, 2) DEFAULT 0,
    net_disbursement DECIMAL(18, 2),

    -- Validity
    valid_from TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Status
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'SUPERSEDED')),
    accepted_at TIMESTAMP WITH TIME ZONE,
    accepted_by UUID REFERENCES identity.users(id),

    -- Conditions
    conditions TEXT[],
    special_terms TEXT,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id)
);

CREATE INDEX idx_loan_offers_application ON application.loan_offers(application_id);
CREATE INDEX idx_loan_offers_status ON application.loan_offers(status);

-- -----------------------------------------------------------------------------
-- Table: application.application_status_history
-- Purpose: Track application status changes (state machine history)
-- -----------------------------------------------------------------------------
CREATE TABLE application.application_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    remarks TEXT,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID REFERENCES identity.users(id)
);

CREATE INDEX idx_status_history_application ON application.application_status_history(application_id);
CREATE INDEX idx_status_history_timestamp ON application.application_status_history(changed_at);

-- =============================================================================
-- SCHEMA: DOCUMENT
-- Purpose: Document management and verification
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS document;

-- -----------------------------------------------------------------------------
-- Table: document.document_types
-- Purpose: Define document types and requirements
-- -----------------------------------------------------------------------------
CREATE TABLE document.document_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('KYC', 'INCOME', 'COLLATERAL', 'LEGAL', 'OTHER')),
    description TEXT,
    is_mandatory BOOLEAN DEFAULT FALSE,
    allowed_formats TEXT[] DEFAULT ARRAY['pdf', 'jpg', 'jpeg', 'png'],
    max_size_mb INTEGER DEFAULT 10,
    applicant_type VARCHAR(20)[] DEFAULT ARRAY['PRIMARY', 'CO_APPLICANT', 'GUARANTOR'],
    loan_products UUID[],
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert standard document types
INSERT INTO document.document_types (code, name, category, is_mandatory) VALUES
    ('PAN_CARD', 'PAN Card', 'KYC', TRUE),
    ('AADHAAR_CARD', 'Aadhaar Card', 'KYC', TRUE),
    ('PASSPORT', 'Passport', 'KYC', FALSE),
    ('VOTER_ID', 'Voter ID', 'KYC', FALSE),
    ('DRIVING_LICENSE', 'Driving License', 'KYC', FALSE),
    ('PHOTO', 'Passport Size Photo', 'KYC', TRUE),
    ('SIGNATURE', 'Signature Specimen', 'KYC', TRUE),
    ('SALARY_SLIP', 'Salary Slip (Last 3 months)', 'INCOME', TRUE),
    ('BANK_STATEMENT', 'Bank Statement (Last 6 months)', 'INCOME', TRUE),
    ('ITR', 'Income Tax Return (Last 2 years)', 'INCOME', FALSE),
    ('FORM_16', 'Form 16', 'INCOME', FALSE),
    ('GST_RETURNS', 'GST Returns', 'INCOME', FALSE),
    ('BALANCE_SHEET', 'Balance Sheet & P/L', 'INCOME', FALSE),
    ('PROPERTY_DOCS', 'Property Documents', 'COLLATERAL', FALSE),
    ('VALUATION_REPORT', 'Valuation Report', 'COLLATERAL', FALSE),
    ('INSURANCE_POLICY', 'Insurance Policy', 'COLLATERAL', FALSE),
    ('SANCTION_LETTER', 'Sanction Letter', 'LEGAL', FALSE),
    ('LOAN_AGREEMENT', 'Loan Agreement', 'LEGAL', FALSE);

-- -----------------------------------------------------------------------------
-- Table: document.documents
-- Purpose: Store document metadata
-- -----------------------------------------------------------------------------
CREATE TABLE document.documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,
    applicant_id UUID REFERENCES application.applicants(id) ON DELETE SET NULL,
    document_type_id UUID NOT NULL REFERENCES document.document_types(id),

    -- File Details
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_extension VARCHAR(10) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100),
    storage_path TEXT NOT NULL,
    storage_bucket VARCHAR(100) DEFAULT 'loanflow-documents',

    -- Security
    checksum VARCHAR(64),
    is_encrypted BOOLEAN DEFAULT TRUE,
    encryption_key_id VARCHAR(50),

    -- OCR/Extraction
    ocr_status VARCHAR(20) CHECK (ocr_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'NOT_APPLICABLE')),
    extracted_data JSONB,
    confidence_score DECIMAL(5, 2),

    -- Verification
    verification_status VARCHAR(20) DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES identity.users(id),
    rejection_reason TEXT,

    -- Metadata
    document_date DATE,
    expiry_date DATE,
    reference_number VARCHAR(100),

    -- Timestamps
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    uploaded_by UUID REFERENCES identity.users(id),

    -- Soft Delete
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES identity.users(id)
);

CREATE INDEX idx_documents_application ON document.documents(application_id);
CREATE INDEX idx_documents_applicant ON document.documents(applicant_id);
CREATE INDEX idx_documents_type ON document.documents(document_type_id);
CREATE INDEX idx_documents_verification ON document.documents(verification_status);

-- -----------------------------------------------------------------------------
-- Table: document.document_verifications
-- Purpose: Track document verification history
-- -----------------------------------------------------------------------------
CREATE TABLE document.document_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES document.documents(id) ON DELETE CASCADE,
    verification_type VARCHAR(30) NOT NULL CHECK (verification_type IN ('MANUAL', 'AUTOMATED', 'THIRD_PARTY')),
    verification_source VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PASSED', 'FAILED', 'INCONCLUSIVE')),
    remarks TEXT,
    verification_data JSONB,
    verified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    verified_by UUID REFERENCES identity.users(id)
);

CREATE INDEX idx_doc_verifications_document ON document.document_verifications(document_id);

-- =============================================================================
-- SCHEMA: COMPLIANCE
-- Purpose: Regulatory compliance, KYC, CERSAI, and audit
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS compliance;

-- -----------------------------------------------------------------------------
-- Table: compliance.kyc_records
-- Purpose: Store KYC verification records
-- -----------------------------------------------------------------------------
CREATE TABLE compliance.kyc_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,
    applicant_id UUID NOT NULL REFERENCES application.applicants(id) ON DELETE CASCADE,

    -- KYC Type
    kyc_type VARCHAR(30) NOT NULL CHECK (kyc_type IN ('CKYC', 'EKYC_AADHAAR', 'EKYC_PAN', 'VIDEO_KYC', 'BIOMETRIC', 'MANUAL')),

    -- CKYC Details
    ckyc_number VARCHAR(20),
    ckyc_verified_at TIMESTAMP WITH TIME ZONE,

    -- e-KYC (Aadhaar)
    aadhaar_reference_number VARCHAR(50),
    ekyc_txn_id VARCHAR(100),
    ekyc_timestamp TIMESTAMP WITH TIME ZONE,
    ekyc_response_code VARCHAR(10),

    -- Verification Result
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VERIFIED', 'FAILED', 'EXPIRED')),
    verification_date DATE,
    expiry_date DATE,

    -- Risk Flags
    pep_check_status VARCHAR(20),
    sanction_check_status VARCHAR(20),
    negative_list_check_status VARCHAR(20),

    -- Response Data (encrypted)
    response_data JSONB,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kyc_application ON compliance.kyc_records(application_id);
CREATE INDEX idx_kyc_applicant ON compliance.kyc_records(applicant_id);
CREATE INDEX idx_kyc_ckyc_number ON compliance.kyc_records(ckyc_number);

-- -----------------------------------------------------------------------------
-- Table: compliance.cersai_registrations
-- Purpose: Track CERSAI charge registrations
-- -----------------------------------------------------------------------------
CREATE TABLE compliance.cersai_registrations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,
    collateral_id UUID NOT NULL REFERENCES application.collaterals(id) ON DELETE CASCADE,

    -- Registration Details
    registration_type VARCHAR(30) NOT NULL CHECK (registration_type IN ('CREATION', 'MODIFICATION', 'SATISFACTION')),
    asset_type VARCHAR(30) NOT NULL,

    -- CERSAI Response
    cersai_registration_number VARCHAR(50),
    cersai_registration_date DATE,
    cersai_txn_id VARCHAR(100),

    -- Status
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUBMITTED', 'REGISTERED', 'REJECTED', 'SATISFIED')),
    rejection_reason TEXT,

    -- Filing Details
    filing_date DATE,
    filing_due_date DATE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cersai_application ON compliance.cersai_registrations(application_id);
CREATE INDEX idx_cersai_collateral ON compliance.cersai_registrations(collateral_id);

-- -----------------------------------------------------------------------------
-- Table: compliance.credit_bureau_reports
-- Purpose: Store credit bureau inquiry results
-- -----------------------------------------------------------------------------
CREATE TABLE compliance.credit_bureau_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES application.loan_applications(id) ON DELETE CASCADE,
    applicant_id UUID NOT NULL REFERENCES application.applicants(id) ON DELETE CASCADE,

    -- Bureau Details
    bureau_name VARCHAR(20) NOT NULL CHECK (bureau_name IN ('CIBIL', 'EXPERIAN', 'EQUIFAX', 'CRIF')),
    inquiry_type VARCHAR(30) DEFAULT 'SOFT' CHECK (inquiry_type IN ('SOFT', 'HARD')),

    -- Request Details
    request_reference VARCHAR(100),
    request_timestamp TIMESTAMP WITH TIME ZONE,

    -- Response Details
    response_reference VARCHAR(100),
    response_timestamp TIMESTAMP WITH TIME ZONE,

    -- Score
    credit_score INTEGER,
    score_version VARCHAR(20),
    score_factors TEXT[],

    -- Summary
    total_accounts INTEGER,
    active_accounts INTEGER,
    overdue_accounts INTEGER,
    written_off_accounts INTEGER,
    total_outstanding DECIMAL(18, 2),
    total_overdue DECIMAL(18, 2),
    dpd_30_plus_count INTEGER,
    dpd_90_plus_count INTEGER,
    enquiry_count_30_days INTEGER,
    enquiry_count_90_days INTEGER,

    -- Full Report (encrypted)
    report_data JSONB,

    -- Status
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'TIMEOUT')),
    error_message TEXT,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_credit_bureau_application ON compliance.credit_bureau_reports(application_id);
CREATE INDEX idx_credit_bureau_applicant ON compliance.credit_bureau_reports(applicant_id);
CREATE INDEX idx_credit_bureau_score ON compliance.credit_bureau_reports(credit_score);

-- -----------------------------------------------------------------------------
-- Table: compliance.audit_logs
-- Purpose: Immutable audit trail for all system activities
-- -----------------------------------------------------------------------------
CREATE TABLE compliance.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Event Details
    event_type VARCHAR(50) NOT NULL,
    event_category VARCHAR(30) NOT NULL CHECK (event_category IN ('AUTH', 'APPLICATION', 'DOCUMENT', 'DECISION', 'SYSTEM', 'DATA_ACCESS')),

    -- Entity Reference
    entity_type VARCHAR(50),
    entity_id UUID,
    entity_number VARCHAR(50),

    -- Actor
    actor_id UUID,
    actor_type VARCHAR(20) DEFAULT 'USER' CHECK (actor_type IN ('USER', 'SYSTEM', 'API', 'JOB')),
    actor_email VARCHAR(255),
    actor_ip VARCHAR(45),
    actor_user_agent TEXT,

    -- Change Details
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'APPROVE', 'REJECT', 'SUBMIT', 'OTHER')),
    description TEXT,
    old_values JSONB,
    new_values JSONB,

    -- Context
    session_id VARCHAR(100),
    request_id VARCHAR(100),
    module VARCHAR(50),

    -- Timestamp (immutable)
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Integrity
    checksum VARCHAR(64)
);

-- Partition audit_logs by month for better performance
-- CREATE TABLE compliance.audit_logs_y2025m01 PARTITION OF compliance.audit_logs
--     FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE INDEX idx_audit_logs_timestamp ON compliance.audit_logs(event_timestamp);
CREATE INDEX idx_audit_logs_entity ON compliance.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON compliance.audit_logs(actor_id);
CREATE INDEX idx_audit_logs_event_type ON compliance.audit_logs(event_type);

-- Prevent updates/deletes on audit_logs
CREATE OR REPLACE FUNCTION compliance.prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be modified';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_audit_update
    BEFORE UPDATE ON compliance.audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION compliance.prevent_audit_modification();

CREATE TRIGGER trg_prevent_audit_delete
    BEFORE DELETE ON compliance.audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION compliance.prevent_audit_modification();

-- =============================================================================
-- SCHEMA: NOTIFICATION
-- Purpose: Notification templates and logs
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS notification;

-- -----------------------------------------------------------------------------
-- Table: notification.templates
-- Purpose: Store notification templates
-- -----------------------------------------------------------------------------
CREATE TABLE notification.templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('APPLICATION', 'DOCUMENT', 'DECISION', 'REMINDER', 'SYSTEM')),
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP')),

    -- Template Content
    subject VARCHAR(500),
    body TEXT NOT NULL,

    -- Variables
    variables TEXT[],

    -- Settings
    is_active BOOLEAN DEFAULT TRUE,
    priority VARCHAR(10) DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),

    -- Localization
    language VARCHAR(10) DEFAULT 'en',

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES identity.users(id),
    updated_by UUID REFERENCES identity.users(id)
);

-- Insert standard templates
INSERT INTO notification.templates (code, name, category, channel, subject, body, variables) VALUES
    ('APP_SUBMITTED', 'Application Submitted', 'APPLICATION', 'EMAIL',
     'Your Loan Application {{application_number}} has been submitted',
     'Dear {{applicant_name}},\n\nYour loan application {{application_number}} for {{product_name}} has been successfully submitted.\n\nAmount: Rs. {{requested_amount}}\n\nWe will review your application and get back to you soon.\n\nRegards,\nLoanFlow Team',
     ARRAY['applicant_name', 'application_number', 'product_name', 'requested_amount']),

    ('APP_APPROVED', 'Application Approved', 'DECISION', 'EMAIL',
     'Congratulations! Your Loan Application {{application_number}} is Approved',
     'Dear {{applicant_name}},\n\nWe are pleased to inform you that your loan application {{application_number}} has been approved.\n\nApproved Amount: Rs. {{approved_amount}}\nInterest Rate: {{interest_rate}}%\nTenure: {{tenure_months}} months\nEMI: Rs. {{emi}}\n\nPlease log in to accept the offer and complete the disbursement process.\n\nRegards,\nLoanFlow Team',
     ARRAY['applicant_name', 'application_number', 'approved_amount', 'interest_rate', 'tenure_months', 'emi']),

    ('APP_REJECTED', 'Application Rejected', 'DECISION', 'EMAIL',
     'Update on Your Loan Application {{application_number}}',
     'Dear {{applicant_name}},\n\nWe regret to inform you that your loan application {{application_number}} could not be approved at this time.\n\nReason: {{rejection_reason}}\n\nYou may reapply after 6 months or contact our support team for more information.\n\nRegards,\nLoanFlow Team',
     ARRAY['applicant_name', 'application_number', 'rejection_reason']),

    ('DOC_PENDING', 'Documents Pending', 'DOCUMENT', 'SMS',
     NULL,
     'Dear {{applicant_name}}, documents pending for your loan application {{application_number}}. Please upload: {{pending_documents}}. - LoanFlow',
     ARRAY['applicant_name', 'application_number', 'pending_documents']);

-- -----------------------------------------------------------------------------
-- Table: notification.logs
-- Purpose: Track sent notifications
-- -----------------------------------------------------------------------------
CREATE TABLE notification.logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID REFERENCES notification.templates(id),

    -- Reference
    application_id UUID REFERENCES application.loan_applications(id),
    applicant_id UUID REFERENCES application.applicants(id),
    user_id UUID REFERENCES identity.users(id),

    -- Channel Details
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP')),
    recipient VARCHAR(255) NOT NULL,

    -- Content
    subject VARCHAR(500),
    body TEXT NOT NULL,

    -- Status
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED')),
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,

    -- Response
    provider_reference VARCHAR(100),
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_logs_application ON notification.logs(application_id);
CREATE INDEX idx_notification_logs_status ON notification.logs(status);
CREATE INDEX idx_notification_logs_created ON notification.logs(created_at);

-- =============================================================================
-- COMMON FUNCTIONS
-- =============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to all relevant tables
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'identity.users', 'identity.roles',
        'application.loan_products', 'application.loan_applications',
        'application.applicants', 'application.addresses',
        'application.employment_details', 'application.collaterals',
        'document.document_types', 'document.documents',
        'compliance.kyc_records', 'compliance.cersai_registrations',
        'notification.templates'
    ]
    LOOP
        EXECUTE format('
            CREATE TRIGGER trg_update_timestamp
            BEFORE UPDATE ON %s
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column()', t);
    END LOOP;
END;
$$;

-- =============================================================================
-- VIEWS
-- =============================================================================

-- View: Application Dashboard Summary
CREATE OR REPLACE VIEW application.v_application_summary AS
SELECT
    la.id,
    la.application_number,
    la.status,
    la.requested_amount,
    la.approved_amount,
    la.created_at,
    lp.code as product_code,
    lp.name as product_name,
    a.first_name || ' ' || a.last_name as applicant_name,
    a.mobile_primary as applicant_mobile,
    la.branch_code
FROM application.loan_applications la
JOIN application.loan_products lp ON la.product_id = lp.id
LEFT JOIN application.applicants a ON la.id = a.application_id AND a.applicant_type = 'PRIMARY'
WHERE la.is_deleted = FALSE;

-- View: Pending Tasks for Underwriters
CREATE OR REPLACE VIEW application.v_pending_review AS
SELECT
    la.id,
    la.application_number,
    la.status,
    la.requested_amount,
    la.submitted_at,
    lp.name as product_name,
    a.first_name || ' ' || a.last_name as applicant_name,
    cbr.credit_score,
    EXTRACT(DAY FROM (CURRENT_TIMESTAMP - la.submitted_at)) as pending_days
FROM application.loan_applications la
JOIN application.loan_products lp ON la.product_id = lp.id
LEFT JOIN application.applicants a ON la.id = a.application_id AND a.applicant_type = 'PRIMARY'
LEFT JOIN compliance.credit_bureau_reports cbr ON la.id = cbr.application_id AND a.id = cbr.applicant_id
WHERE la.status IN ('UNDER_REVIEW', 'PENDING_APPROVAL')
AND la.is_deleted = FALSE
ORDER BY la.submitted_at ASC;

-- =============================================================================
-- GRANTS (adjust as per environment)
-- =============================================================================
-- GRANT USAGE ON SCHEMA identity, application, document, compliance, notification TO loanflow_app;
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA identity, application, document, notification TO loanflow_app;
-- GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA compliance TO loanflow_app;
-- GRANT USAGE ON ALL SEQUENCES IN SCHEMA application TO loanflow_app;

-- =============================================================================
-- END OF SCHEMA
-- =============================================================================
