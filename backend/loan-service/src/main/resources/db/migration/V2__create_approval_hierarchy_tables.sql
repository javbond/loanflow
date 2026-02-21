-- V2__create_approval_hierarchy_tables.sql
-- US-015: Approval Hierarchy — amount-based approval authority matrix
-- and delegation of authority support.

-- ========================================================================
-- APPROVAL AUTHORITY MATRIX
-- Defines amount-based tiers for each loan type with required approval role.
-- ========================================================================
CREATE TABLE application.approval_authority (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_type VARCHAR(30),
    tier_level INTEGER NOT NULL,
    tier_name VARCHAR(50) NOT NULL,
    min_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    max_amount DECIMAL(15, 2),
    required_role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_approval_loan_type CHECK (
        loan_type IS NULL OR loan_type IN (
            'HOME_LOAN', 'PERSONAL_LOAN', 'VEHICLE_LOAN',
            'BUSINESS_LOAN', 'EDUCATION_LOAN', 'GOLD_LOAN', 'LAP'
        )
    ),
    CONSTRAINT chk_approval_role CHECK (
        required_role IN ('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')
    ),
    CONSTRAINT chk_approval_amounts CHECK (
        min_amount >= 0 AND (max_amount IS NULL OR max_amount > min_amount)
    ),
    CONSTRAINT uq_approval_auth_type_tier UNIQUE (loan_type, tier_level)
);

CREATE INDEX idx_approval_auth_loan_type ON application.approval_authority(loan_type);
CREATE INDEX idx_approval_auth_role ON application.approval_authority(required_role);
CREATE INDEX idx_approval_auth_active ON application.approval_authority(active);

-- Auto-update updated_at trigger
CREATE TRIGGER update_approval_authority_updated_at
    BEFORE UPDATE ON application.approval_authority
    FOR EACH ROW
    EXECUTE FUNCTION application.update_updated_at_column();

COMMENT ON TABLE application.approval_authority IS 'Approval authority matrix — amount-based tiers per loan type';
COMMENT ON COLUMN application.approval_authority.loan_type IS 'Loan type this tier applies to. NULL = applies to all types';
COMMENT ON COLUMN application.approval_authority.tier_level IS 'Tier ordering (1=lowest authority, higher=more authority)';
COMMENT ON COLUMN application.approval_authority.required_role IS 'Flowable candidate group / Keycloak role required for approval';

-- ========================================================================
-- DELEGATION OF AUTHORITY
-- Temporary authority grants from senior to junior officers.
-- ========================================================================
CREATE TABLE application.delegation_of_authority (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delegator_id UUID NOT NULL,
    delegator_role VARCHAR(30) NOT NULL,
    delegatee_id UUID NOT NULL,
    delegatee_role VARCHAR(30) NOT NULL,
    max_amount DECIMAL(15, 2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,

    CONSTRAINT chk_doa_roles CHECK (
        delegator_role IN ('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')
        AND delegatee_role IN ('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')
    ),
    CONSTRAINT chk_doa_dates CHECK (valid_to >= valid_from),
    CONSTRAINT chk_doa_amount CHECK (max_amount > 0),
    CONSTRAINT chk_doa_different_officers CHECK (delegator_id != delegatee_id)
);

CREATE INDEX idx_doa_delegatee ON application.delegation_of_authority(delegatee_id);
CREATE INDEX idx_doa_delegator ON application.delegation_of_authority(delegator_id);
CREATE INDEX idx_doa_active ON application.delegation_of_authority(active);
CREATE INDEX idx_doa_valid_period ON application.delegation_of_authority(valid_from, valid_to);

-- Auto-update updated_at trigger
CREATE TRIGGER update_delegation_of_authority_updated_at
    BEFORE UPDATE ON application.delegation_of_authority
    FOR EACH ROW
    EXECUTE FUNCTION application.update_updated_at_column();

COMMENT ON TABLE application.delegation_of_authority IS 'Temporary delegation of approval authority between officers';

-- ========================================================================
-- SEED DEFAULT APPROVAL MATRIX
-- Default tiers applicable to ALL loan types (loan_type IS NULL).
-- Product-specific overrides can be added later via the API.
-- ========================================================================

-- Tier 1: Loan Officer — up to ₹5,00,000 (5 Lakh)
INSERT INTO application.approval_authority (loan_type, tier_level, tier_name, min_amount, max_amount, required_role)
VALUES (NULL, 1, 'Loan Officer Approval', 0, 500000, 'LOAN_OFFICER');

-- Tier 2: Underwriter — ₹5,00,001 to ₹25,00,000 (25 Lakh)
INSERT INTO application.approval_authority (loan_type, tier_level, tier_name, min_amount, max_amount, required_role)
VALUES (NULL, 2, 'Underwriter Approval', 500001, 2500000, 'UNDERWRITER');

-- Tier 3: Senior Underwriter — ₹25,00,001 to ₹1,00,00,000 (1 Crore)
INSERT INTO application.approval_authority (loan_type, tier_level, tier_name, min_amount, max_amount, required_role)
VALUES (NULL, 3, 'Senior Underwriter Approval', 2500001, 10000000, 'SENIOR_UNDERWRITER');

-- Tier 4: Branch Manager — above ₹1,00,00,000 (1 Crore+)
INSERT INTO application.approval_authority (loan_type, tier_level, tier_name, min_amount, max_amount, required_role)
VALUES (NULL, 4, 'Branch Manager Approval', 10000001, NULL, 'BRANCH_MANAGER');

-- ========================================================================
-- HOME LOAN SPECIFIC OVERRIDES (higher limits due to secured nature)
-- ========================================================================

-- Tier 1: Underwriter — up to ₹50,00,000 (50 Lakh)
INSERT INTO application.approval_authority (loan_type, tier_level, tier_name, min_amount, max_amount, required_role)
VALUES ('HOME_LOAN', 1, 'HL Underwriter Approval', 0, 5000000, 'UNDERWRITER');

-- Tier 2: Senior Underwriter — ₹50,00,001 to ₹2,00,00,000 (2 Crore)
INSERT INTO application.approval_authority (loan_type, tier_level, tier_name, min_amount, max_amount, required_role)
VALUES ('HOME_LOAN', 2, 'HL Senior Underwriter Approval', 5000001, 20000000, 'SENIOR_UNDERWRITER');

-- Tier 3: Branch Manager — above ₹2,00,00,000 (2 Crore+)
INSERT INTO application.approval_authority (loan_type, tier_level, tier_name, min_amount, max_amount, required_role)
VALUES ('HOME_LOAN', 3, 'HL Branch Manager Approval', 20000001, NULL, 'BRANCH_MANAGER');
