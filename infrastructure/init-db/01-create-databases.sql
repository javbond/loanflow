-- Create databases for each service
CREATE DATABASE IF NOT EXISTS loanflow_customer;
CREATE DATABASE IF NOT EXISTS loanflow_loan;
CREATE DATABASE IF NOT EXISTS loanflow_auth;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE loanflow_customer TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE loanflow_loan TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE loanflow_auth TO loanflow;
