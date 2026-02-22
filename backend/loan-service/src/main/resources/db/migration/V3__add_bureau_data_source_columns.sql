-- V3__add_bureau_data_source_columns.sql
-- US-016: Credit Bureau Integration — track bureau data provenance

ALTER TABLE application.loan_applications
    ADD COLUMN bureau_data_source VARCHAR(20),
    ADD COLUMN bureau_pull_timestamp TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN application.loan_applications.bureau_data_source IS 'Source of credit bureau data: REAL, CACHED, or SIMULATED';
COMMENT ON COLUMN application.loan_applications.bureau_pull_timestamp IS 'Timestamp of the credit bureau data pull';

-- Add CHECK constraint for valid values
ALTER TABLE application.loan_applications
    ADD CONSTRAINT chk_bureau_data_source
    CHECK (bureau_data_source IS NULL OR bureau_data_source IN ('REAL', 'CACHED', 'SIMULATED'));
