-- LoanFlow PostgreSQL Initialization
-- Creates databases for each microservice

-- Create databases
CREATE DATABASE loan_db;
CREATE DATABASE customer_db;
CREATE DATABASE auth_db;
CREATE DATABASE workflow_db;
CREATE DATABASE notification_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE loan_db TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE customer_db TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE auth_db TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE workflow_db TO loanflow;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO loanflow;

-- Setup loan_db
\c loan_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE SCHEMA IF NOT EXISTS application;
CREATE SCHEMA IF NOT EXISTS compliance;

-- Setup customer_db
\c customer_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS identity;

-- Setup workflow_db
\c workflow_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
