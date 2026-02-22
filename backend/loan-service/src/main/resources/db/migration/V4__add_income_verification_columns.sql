-- V4: Add income verification columns (US-017)
-- Stores income verification results from ITR/GST/bank statement analysis

ALTER TABLE application.loan_applications
    ADD COLUMN income_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN verified_monthly_income DECIMAL(15,2),
    ADD COLUMN dti_ratio DECIMAL(5,4),
    ADD COLUMN income_data_source VARCHAR(20);

-- Constraint to ensure valid income data source values
ALTER TABLE application.loan_applications
    ADD CONSTRAINT chk_income_data_source
    CHECK (income_data_source IS NULL OR income_data_source IN ('REAL', 'CACHED', 'SIMULATED'));

COMMENT ON COLUMN application.loan_applications.income_verified IS 'Whether income was successfully verified';
COMMENT ON COLUMN application.loan_applications.verified_monthly_income IS 'Verified monthly income from ITR/bank statements';
COMMENT ON COLUMN application.loan_applications.dti_ratio IS 'Debt-to-Income ratio (0.0000 to 1.0000)';
COMMENT ON COLUMN application.loan_applications.income_data_source IS 'Income verification data source: REAL, CACHED, or SIMULATED';
