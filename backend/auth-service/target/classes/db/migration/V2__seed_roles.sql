-- Seed default roles
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Administrator with full access'),
    ('LOAN_OFFICER', 'Creates and manages loan applications'),
    ('UNDERWRITER', 'Reviews and decides on loan applications'),
    ('SENIOR_UNDERWRITER', 'Senior approval authority for higher limits'),
    ('CUSTOMER', 'End user/applicant')
ON CONFLICT (name) DO NOTHING;
