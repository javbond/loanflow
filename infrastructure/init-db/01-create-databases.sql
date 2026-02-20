-- Create databases for each service
-- Names must match application.yml defaults: customer_db, loan_db
CREATE DATABASE customer_db;
CREATE DATABASE loan_db;
CREATE DATABASE loanflow_auth;
CREATE DATABASE keycloak;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE customer_db TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE loan_db TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE loanflow_auth TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO loanflow;
