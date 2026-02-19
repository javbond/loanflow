-- Create databases for each service
CREATE DATABASE loanflow_customer;
CREATE DATABASE loanflow_loan;
CREATE DATABASE loanflow_auth;
CREATE DATABASE keycloak;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE loanflow_customer TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE loanflow_loan TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE loanflow_auth TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO loanflow;
