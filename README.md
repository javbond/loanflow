# LoanFlow - Loan Origination System

A production-grade Loan Origination System for Indian banks, built with Spring Boot microservices, Angular 17, Flowable BPMN, Drools decision engine, and Keycloak authentication.

## Architecture

```
                    +-----------+
                    |  Angular  |
                    | Frontend  |
                    |  :4200    |
                    +-----+-----+
                          |
                    +-----v-----+
                    |    API    |
                    |  Gateway  |
                    |   :8080   |
                    +-----+-----+
                          |
        +---------+-------+-------+---------+---------+
        |         |       |       |         |         |
   +----v---+ +---v----+ +v------+ +-------v+ +------v------+
   |  Auth  | |Customer| | Loan  | |Document| |Notification |
   |Service | |Service | |Service| |Service | |  Service    |
   | :8085  | | :8082  | | :8081 | | :8083  | |   :8084     |
   +--------+ +--------+ +---+---+ +--------+ +-------------+
                              |
                         +----v----+
                         | Policy  |
                         | Service |
                         |  :8086  |
                         +---------+
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 17, Angular Material, TypeScript |
| API Gateway | Spring Cloud Gateway (reactive) |
| Backend | Spring Boot 3.2.2, Java 20 |
| Workflow | Flowable BPMN 7.1.0 |
| Decision Engine | Drools 9.44.0 |
| Auth | Keycloak 23.0.3 (OAuth2/OIDC) |
| Databases | PostgreSQL 16, MongoDB 7 |
| Object Storage | MinIO (S3-compatible) |
| Messaging | RabbitMQ 3 |
| Caching | Redis 7 |
| Virus Scan | ClamAV 1.2 |

## Services

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| api-gateway | 8080 | - | Spring Cloud Gateway, JWT relay, route management |
| loan-service | 8081 | PostgreSQL `loan_db` | Core loan processing, Flowable BPMN, Drools |
| customer-service | 8082 | PostgreSQL `customer_db` | Customer management, e-KYC |
| document-service | 8083 | MongoDB `document_db` | Document storage (MinIO), OCR, verification |
| notification-service | 8084 | - | Event-driven notifications (email/SMS via RabbitMQ) |
| auth-service | 8085 | - | Keycloak OAuth2/OIDC integration |
| policy-service | 8086 | MongoDB `policy_db` | Dynamic policy engine, Redis caching |

## Quick Start

### Prerequisites

- Java 20
- Node.js 20
- Docker & Docker Compose

### 1. Start Infrastructure

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

This starts PostgreSQL, MongoDB, MinIO, Redis, RabbitMQ, Keycloak, and ClamAV.

### 2. Build Backend

```bash
export JAVA_HOME=/path/to/java-20
cd backend
mvn clean install -DskipTests
```

Or use the build script:

```bash
./scripts/build-all.sh
```

### 3. Start Services

Start each service individually or use Docker Compose:

```bash
# Individual (from backend/)
cd loan-service && mvn spring-boot:run
cd customer-service && mvn spring-boot:run
# ... etc

# Or via Docker Compose (all services)
docker compose -f infrastructure/docker-compose.yml \
               -f infrastructure/docker-compose.services.yml up -d
```

### 4. Start Frontend

```bash
cd frontend/loanflow-web
npm install
npm start
```

Access at http://localhost:4200

### 5. Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| Keycloak Admin | http://localhost:8180 (admin/admin) |
| RabbitMQ Console | http://localhost:15672 (loanflow/loanflow_secret) |
| MinIO Console | http://localhost:9001 (loanflow/loanflow_secret) |

## Test Users

| Role | Email | Password |
|------|-------|----------|
| Customer | customer@example.com | customer123 |
| Loan Officer | officer@loanflow.com | officer123 |
| Underwriter | underwriter@loanflow.com | underwriter123 |

## Running Tests

```bash
# All tests
export JAVA_HOME=/path/to/java-20
cd backend
mvn test

# Integration tests only (requires Docker for Testcontainers)
mvn test -Dtest="*IntegrationTest"
```

## Project Structure

```
loanflow/
  backend/
    api-gateway/           # Spring Cloud Gateway
    auth-service/          # Keycloak integration
    customer-service/      # Customer management
    document-service/      # Document management
    loan-service/          # Core loan processing
    notification-service/  # Event-driven notifications
    policy-service/        # Policy engine
    loanflow-common/       # Shared modules (dto, security, utils)
  frontend/
    loanflow-web/          # Angular 17 application
  infrastructure/
    docker-compose.yml          # Infrastructure services
    docker-compose.services.yml # Application services
    dockerfiles/                # Dockerfile templates
    keycloak/                   # Realm configuration
    nginx/                      # Nginx config for production
  docs/
    sprints/               # Sprint plans and reviews
    prd/                   # Product Requirements
  scripts/
    build-all.sh           # Full build script
```

## Sprint History

| Sprint | Stories | Points | Status |
|--------|---------|--------|--------|
| 1 | Project Setup, Customer Module | 16 | ✅ |
| 2 | Loan & Document Management UI | 13 | ✅ |
| 3 | Authentication & RBAC | 16 | ✅ |
| 4 | Customer Self-Service Portal | 21 | ✅ |
| 5 | Policy Engine Foundation | 13 | ✅ |
| 6 | Advanced Policy Features | 16 | ✅ |
| 7 | Drools + Approval Hierarchy + Risk Dashboard | 16 | ✅ |
| 8 | CIBIL + Income Verification + Enhanced Upload | 16 | ✅ |
| 9 | Document Lifecycle (Verification, OCR, Sanction) | 16 | ✅ |
| 10 | Regulatory Compliance (e-KYC, Audit, Notifications) | 16 | ✅ |
| 11 | Hardening & Production Readiness | 16 | In Progress |
