-- Seed default admin user
-- Password: Admin@123 (BCrypt encoded)
INSERT INTO users (id, email, password, first_name, last_name, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@loanflow.com',
    '$2a$10$rDkPvvAFV8kqwvKJzwlRv.7H8QbZo9Xq9wXmjrqFT6PLxR8ZxZ.Vy',
    'System',
    'Admin',
    true
) ON CONFLICT (email) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT
    '00000000-0000-0000-0000-000000000001',
    r.id
FROM roles r
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Seed test users for UAT
-- Password: Test@123 (BCrypt encoded)
INSERT INTO users (id, email, password, first_name, last_name, enabled)
VALUES
    ('00000000-0000-0000-0000-000000000002', 'officer@loanflow.com', '$2a$10$rDkPvvAFV8kqwvKJzwlRv.7H8QbZo9Xq9wXmjrqFT6PLxR8ZxZ.Vy', 'Loan', 'Officer', true),
    ('00000000-0000-0000-0000-000000000003', 'underwriter@loanflow.com', '$2a$10$rDkPvvAFV8kqwvKJzwlRv.7H8QbZo9Xq9wXmjrqFT6PLxR8ZxZ.Vy', 'Senior', 'Underwriter', true)
ON CONFLICT (email) DO NOTHING;

-- Assign roles to test users
INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000002', r.id FROM roles r WHERE r.name = 'LOAN_OFFICER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000003', r.id FROM roles r WHERE r.name = 'UNDERWRITER'
ON CONFLICT DO NOTHING;
