-- V5: Add customer_email column to loan_applications
-- Required by LoanApplication entity for customer communication and notification linkage

ALTER TABLE application.loan_applications
    ADD COLUMN IF NOT EXISTS customer_email VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_loan_app_customer_email
    ON application.loan_applications(customer_email);

COMMENT ON COLUMN application.loan_applications.customer_email IS 'Email address of the loan applicant';
