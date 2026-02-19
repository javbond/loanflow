# LoanFlow Container Architecture Guide

## End-to-End Use Case: Home Loan Application Flow

This document explains how each Docker container in LoanFlow works together through a real-world use case.

---

## Use Case: Customer Applies for a Home Loan (₹50 Lakhs)

Let's follow **Rahul Sharma** as he applies for a home loan through LoanFlow.

---

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    LOANFLOW ARCHITECTURE                                 │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌──────────┐     ┌─────────────┐     ┌─────────────────────────────────────────────┐  │
│  │  Rahul   │────▶│   Angular   │────▶│              SPRING BOOT GATEWAY            │  │
│  │ (Browser)│     │   Frontend  │     │                  (Port 8080)                │  │
│  └──────────┘     └─────────────┘     └───────────────────┬─────────────────────────┘  │
│                                                           │                             │
│                          ┌────────────────────────────────┼────────────────────────┐   │
│                          │                                │                        │   │
│                          ▼                                ▼                        ▼   │
│  ┌───────────────────────────────┐  ┌─────────────────────────────┐  ┌────────────────┐│
│  │         KEYCLOAK              │  │      MICROSERVICES          │  │   RABBITMQ     ││
│  │        (Port 8180)            │  │                             │  │  (Port 5672)   ││
│  │  ┌─────────────────────────┐  │  │  ┌─────────────────────┐   │  │                ││
│  │  │ • OAuth2/OIDC Auth      │  │  │  │ loan-service        │   │  │  ┌──────────┐  ││
│  │  │ • JWT Token Issuance    │  │  │  │ customer-service    │   │  │  │ Queues:  │  ││
│  │  │ • Role Management       │  │  │  │ document-service    │   │  │  │ • loan   │  ││
│  │  │ • SSO Support           │  │  │  │ notification-svc    │   │  │  │ • notif  │  ││
│  │  └─────────────────────────┘  │  │  │ credit-service      │   │  │  │ • docs   │  ││
│  └───────────────────────────────┘  │  └──────────┬──────────┘   │  │  └──────────┘  ││
│                                     └─────────────┼──────────────┘  └───────┬────────┘│
│                                                   │                         │         │
│       ┌───────────────────────────────────────────┼─────────────────────────┘         │
│       │                                           │                                    │
│       ▼                                           ▼                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │   POSTGRESQL    │  │    MONGODB      │  │     REDIS       │  │     MINIO       │   │
│  │   (Port 5432)   │  │  (Port 27017)   │  │   (Port 6379)   │  │  (Port 9000)    │   │
│  │                 │  │                 │  │                 │  │                 │   │
│  │ • loan_db       │  │ • Policies      │  │ • Session Cache │  │ • KYC Docs      │   │
│  │ • customer_db   │  │ • Credit Cache  │  │ • Rate Limits   │  │ • Loan Docs     │   │
│  │ • workflow_db   │  │ • Audit Logs    │  │ • OTP Storage   │  │ • Templates     │   │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘   │
│                              │                                                         │
│       ┌──────────────────────┘                                                         │
│       │                                                                                │
│       ▼                                                                                │
│  ┌─────────────────────────────────────────┐                                          │
│  │           FLOWABLE ENGINE               │                                          │
│  │      (REST: 8085 | UI: 8086)            │                                          │
│  │                                         │                                          │
│  │  ┌───────────────────────────────────┐  │                                          │
│  │  │ BPMN Workflow States:             │  │                                          │
│  │  │ DRAFT → SUBMITTED → VERIFICATION  │  │                                          │
│  │  │ → CREDIT_CHECK → UNDERWRITING     │  │                                          │
│  │  │ → APPROVAL → DISBURSEMENT         │  │                                          │
│  │  └───────────────────────────────────┘  │                                          │
│  └─────────────────────────────────────────┘                                          │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Flow

### STEP 1: Login & Authentication

**Flow:** `Rahul → Angular App → Keycloak → JWT Token → Angular App`

| Container | Role |
|-----------|------|
| **Keycloak** (8180) | Handles OAuth2 login, validates credentials, issues JWT token with roles |
| **Redis** (6379) | Stores session data, prevents replay attacks |
| **PostgreSQL** (auth_db) | Stores user credentials, role mappings |

```
┌─────────┐      ┌─────────────┐      ┌───────────┐      ┌─────────┐
│  Rahul  │─────▶│ Angular App │─────▶│ Keycloak  │─────▶│ Postgres│
│         │      │             │      │  :8180    │      │ auth_db │
└─────────┘      └─────────────┘      └─────┬─────┘      └─────────┘
                                            │
                        ┌───────────────────┘
                        ▼
                 ┌─────────────┐
                 │    Redis    │  ← Session stored
                 │    :6379    │
                 └─────────────┘
                        │
                        ▼
              JWT Token returned:
              {
                "sub": "rahul.sharma",
                "roles": ["CUSTOMER"],
                "exp": 1708200000
              }
```

**What happens:**
1. Rahul enters username/password in Angular login form
2. Angular redirects to Keycloak login page (OAuth2 Authorization Code flow with PKCE)
3. Keycloak validates credentials against PostgreSQL `auth_db`
4. On success, Keycloak issues a JWT token containing user roles
5. Session is cached in Redis for fast subsequent validations
6. Angular stores the JWT and includes it in all API requests

---

### STEP 2: Start Loan Application

**Flow:** `Angular → API Gateway → loan-service → PostgreSQL + Flowable`

| Container | Role |
|-----------|------|
| **PostgreSQL** (loan_db) | Creates loan application record |
| **Flowable** (8085) | Starts BPMN workflow process instance |
| **MongoDB** | Fetches applicable loan policies (interest rates, eligibility) |

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ Angular App │────▶│ loan-service │────▶│  PostgreSQL │
│             │     │              │     │   loan_db   │
└─────────────┘     └──────┬───────┘     └─────────────┘
                           │                    │
          ┌────────────────┤              Application Created:
          │                │              ID: LN-2024-001234
          ▼                ▼
    ┌───────────┐    ┌─────────────┐
    │  MongoDB  │    │  Flowable   │
    │           │    │    :8085    │
    └───────────┘    └─────────────┘
          │                │
    Policy fetched:   Process Instance:
    - Home Loan       PI-789456
    - 8.5% interest   State: DRAFT
    - Max 80% LTV
```

**What happens:**
1. Rahul fills loan application form (amount: ₹50L, tenure: 20 years, property details)
2. Angular sends POST to `/api/v1/loans` with JWT in Authorization header
3. API Gateway validates JWT with Keycloak
4. `loan-service` fetches applicable policies from MongoDB (interest rates, max LTV, eligibility rules)
5. Creates loan record in PostgreSQL `loan_db`
6. Starts Flowable BPMN process instance - workflow state: `DRAFT`
7. Returns loan application ID: `LN-2024-001234`

---

### STEP 3: KYC Document Upload

**Flow:** `Angular → document-service → MinIO + PostgreSQL`

| Container | Role |
|-----------|------|
| **MinIO** (9000) | Stores actual files (Aadhaar, PAN, salary slips) |
| **PostgreSQL** (document_db) | Stores document metadata, verification status |
| **RabbitMQ** (5672) | Queues OCR extraction job |

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│ Angular App │────▶│ document-service │────▶│    MinIO    │
│             │     │                  │     │    :9000    │
│ Uploads:    │     └────────┬─────────┘     └─────────────┘
│ - Aadhaar   │              │                     │
│ - PAN Card  │              │               Files stored:
│ - Pay Slips │              │               /kyc-documents/
└─────────────┘              │               LN-2024-001234/
                             │               ├── aadhaar.pdf
                             ▼               ├── pan.pdf
                    ┌─────────────────┐      └── salary.pdf
                    │    RabbitMQ     │
                    │     :5672       │
                    └────────┬────────┘
                             │
                    Queue: document.ocr.extract
                    Message: {
                      "docId": "DOC-456",
                      "type": "AADHAAR",
                      "path": "/kyc-documents/..."
                    }
```

**What happens:**
1. Rahul uploads KYC documents (Aadhaar, PAN, 3 months salary slips)
2. `document-service` streams files directly to MinIO (S3-compatible storage)
3. Document metadata saved to PostgreSQL `document_db` (filename, type, upload time, status: PENDING)
4. RabbitMQ message published to `document.ocr.extract` queue
5. Background OCR worker picks up the message, extracts text (Aadhaar number, PAN, salary figures)
6. Extracted data saved to MongoDB for quick retrieval
7. Workflow advances: `DRAFT → SUBMITTED → VERIFICATION`

---

### STEP 4: Credit Bureau Check (CIBIL)

**Flow:** `loan-service → credit-service → External CIBIL API → MongoDB (cache) → PostgreSQL`

| Container | Role |
|-----------|------|
| **MongoDB** | Caches CIBIL response (valid 30 days) |
| **PostgreSQL** | Stores credit decision |
| **Redis** | Rate limiting for CIBIL API calls |
| **Flowable** | Moves workflow to CREDIT_CHECK state |

```
┌──────────────┐     ┌────────────────┐     ┌─────────────┐
│ loan-service │────▶│ credit-service │────▶│    Redis    │
│              │     │                │     │   :6379     │
└──────────────┘     └───────┬────────┘     └─────────────┘
                             │                    │
                    Check rate limit:       ✓ Under limit
                    CIBIL calls < 100/min   (85 calls today)
                             │
                             ▼
                    ┌─────────────────┐     ┌──────────────┐
                    │  External CIBIL │────▶│   MongoDB    │
                    │      API        │     │              │
                    └─────────────────┘     └──────────────┘
                             │                    │
                    Response:              Cached for 30 days:
                    Score: 782             {
                    Risk: LOW                "pan": "ABCDE1234F",
                                             "score": 782,
                             │                "cachedAt": "2024-02-17"
                             ▼              }
                    ┌─────────────────┐
                    │    Flowable     │
                    │                 │
                    │ State Change:   │
                    │ CREDIT_CHECK →  │
                    │ UNDERWRITING    │
                    └─────────────────┘
```

**What happens:**
1. Verification complete triggers credit check
2. `credit-service` first checks Redis for rate limit (max 100 CIBIL calls/minute)
3. Checks MongoDB cache - if recent CIBIL report exists (< 30 days), use cached
4. If not cached, calls external CIBIL API with Rahul's PAN
5. CIBIL returns: Score 782, 2 active loans, no defaults
6. Response cached in MongoDB for future use
7. Credit decision saved to PostgreSQL
8. Flowable workflow advances: `VERIFICATION → CREDIT_CHECK → UNDERWRITING`

---

### STEP 5: Underwriting Decision

**Flow:** `Flowable assigns task → Underwriter reviews → Approves/Rejects`

| Container | Role |
|-----------|------|
| **Flowable UI** (8086) | Underwriter's task inbox, approval forms |
| **Flowable REST** (8085) | Task API, workflow progression |
| **MongoDB** | Stores decision rationale, audit trail |
| **RabbitMQ** | Notifies next steps |

```
┌─────────────────┐     ┌─────────────────┐
│  Flowable UI    │     │ Underwriter     │
│     :8086       │◀────│ (Browser)       │
└────────┬────────┘     └─────────────────┘
         │
         │ Task Inbox shows:
         │ ┌─────────────────────────────┐
         │ │ LN-2024-001234              │
         │ │ Rahul Sharma - ₹50L Home    │
         │ │ CIBIL: 782 | LTV: 75%       │
         │ │ [APPROVE] [REJECT] [QUERY]  │
         │ └─────────────────────────────┘
         │
         ▼ Underwriter clicks APPROVE
┌─────────────────┐     ┌─────────────────┐
│  Flowable REST  │────▶│    MongoDB      │
│     :8085       │     │                 │
└────────┬────────┘     └─────────────────┘
         │                    │
         │              Audit Log:
         │              {
         │                "action": "APPROVE",
         │                "by": "underwriter1",
         │                "reason": "Good credit profile",
         │                "timestamp": "2024-02-17T10:30:00Z"
         │              }
         ▼
┌─────────────────┐
│    RabbitMQ     │
│     :5672       │
└────────┬────────┘
         │
   Queue: loan.approved
   → Triggers disbursement process
   → Triggers customer notification
```

**What happens:**
1. Flowable creates user task assigned to `UNDERWRITER` role
2. Underwriter logs into Flowable UI (http://localhost:8086)
3. Sees task in inbox with all loan details, CIBIL score, documents
4. Reviews and clicks APPROVE with comments
5. Decision and rationale saved to MongoDB audit log (immutable)
6. Flowable completes task, advances workflow: `UNDERWRITING → APPROVED`
7. RabbitMQ messages published:
   - `loan.approved` → triggers disbursement workflow
   - `notification.send` → triggers customer SMS/email

---

### STEP 6: Customer Notification

**Flow:** `RabbitMQ → notification-service → Email/SMS`

| Container | Role |
|-----------|------|
| **RabbitMQ** (5672) | Message queue for async notifications |
| **PostgreSQL** (notification_db) | Stores notification history |
| **Redis** | Deduplication, prevents double-sending |

```
┌─────────────────┐     ┌──────────────────────┐
│    RabbitMQ     │────▶│ notification-service │
│     :5672       │     │                      │
└─────────────────┘     └──────────┬───────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
         ▼                         ▼                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Redis       │     │   PostgreSQL    │     │  External SMS   │
│     :6379       │     │ notification_db │     │  / Email API    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                         │                         │
   Dedupe check:            Record saved:              Message sent:
   Key: notif:LN-001234     {                         "Dear Rahul,
   :APPROVED                  "type": "LOAN_APPROVED", Your home loan
   TTL: 24h                   "channel": "SMS",        LN-2024-001234
                              "status": "SENT"         for ₹50,00,000
                            }                          is approved!"
```

**What happens:**
1. `notification-service` consumes message from RabbitMQ queue
2. Checks Redis for deduplication (prevents sending same SMS twice if retry occurs)
3. Fetches notification template from MongoDB
4. Calls external SMS gateway (MSG91/Twilio) and Email service (SendGrid)
5. Records delivery status in PostgreSQL `notification_db`
6. Rahul receives SMS: "Your home loan LN-2024-001234 for ₹50,00,000 is approved!"

---

## Complete Sequence Diagram

```
    Rahul        Angular       Keycloak      loan-svc     Flowable      Postgres     MongoDB      MinIO       RabbitMQ      Redis
      │            │              │             │            │             │            │           │            │            │
      │──Login────▶│              │             │            │             │            │           │            │            │
      │            │──OAuth2─────▶│             │            │             │            │           │            │            │
      │            │              │──Validate──▶│            │             │            │           │            │            │
      │            │              │             │            │             │◀───────────│           │            │            │
      │            │◀─JWT Token───│             │            │             │            │           │            │──Session──▶│
      │◀───────────│              │             │            │             │            │           │            │            │
      │            │              │             │            │             │            │           │            │            │
      │──Apply────▶│              │             │            │             │            │           │            │            │
      │            │──────────────┼────────────▶│            │             │            │           │            │            │
      │            │              │             │──Start────▶│             │            │           │            │            │
      │            │              │             │            │──Process───▶│            │           │            │            │
      │            │              │             │◀──Policy───┼─────────────┼───────────▶│           │            │            │
      │◀───LN-ID───│              │             │            │             │            │           │            │            │
      │            │              │             │            │             │            │           │            │            │
      │──Upload───▶│              │             │            │             │            │           │            │            │
      │  Docs      │──────────────┼─────────────┼────────────┼─────────────┼────────────┼──Store───▶│            │            │
      │            │              │             │            │             │            │           │──OCR Job──▶│            │
      │◀──Success──│              │             │            │             │            │           │            │            │
      │            │              │             │            │             │            │           │            │            │
      │            │              │             │──CIBIL────▶│             │            │           │            │──RateChk──▶│
      │            │              │             │            │             │            │◀──Cache───│            │            │
      │            │              │             │◀─Score:782─│             │            │           │            │            │
      │            │              │             │──Advance──▶│             │            │           │            │            │
      │            │              │             │            │──State──────▶            │           │            │            │
      │            │              │             │            │             │            │           │            │            │
      │            │              │             │            │             │            │           │            │            │
   [Underwriter reviews in Flowable UI :8086, clicks APPROVE]              │            │           │            │            │
      │            │              │             │            │             │            │           │            │            │
      │            │              │             │◀─Approved──│             │            │           │            │            │
      │            │              │             │            │──Audit─────▶│            │           │            │            │
      │            │              │             │            │             │            │◀──Log─────│            │            │
      │            │              │             │────────────┼─────────────┼────────────┼───────────┼──Notify───▶│            │
      │◀─SMS/Email─│              │             │            │             │            │           │            │◀─Dedupe────│
      │            │              │             │            │             │            │           │            │            │
      ▼            ▼              ▼             ▼            ▼             ▼            ▼           ▼            ▼            ▼
```

---

## Container Summary

| Container | Port | Purpose | When Used |
|-----------|------|---------|-----------|
| **PostgreSQL** | 5432 | Relational data (loans, customers, users) | Every transaction - ACID compliance for financial data |
| **MongoDB** | 27017 | Documents, policies, audit logs, cache | Policy lookup, flexible schema data, caching |
| **Redis** | 6379 | Sessions, rate limits, OTP, fast cache | Auth sessions, API protection, real-time data |
| **Keycloak** | 8180 | Identity & Access Management | Login, token validation, role management |
| **Flowable REST** | 8085 | BPMN workflow engine API | Workflow state management, task creation |
| **Flowable UI** | 8086 | Visual workflow designer + task inbox | Staff approve/reject tasks, design workflows |
| **MinIO** | 9000/9001 | S3-compatible object storage | Document uploads (KYC, agreements, reports) |
| **RabbitMQ** | 5672/15672 | Message queue | Async operations (notifications, OCR, integrations) |

---

## Why Each Container?

| Container | Why Not Just Use One Database? |
|-----------|-------------------------------|
| **PostgreSQL** | ACID transactions for financial data - can't afford to lose a loan record or have inconsistent state |
| **MongoDB** | Flexible schema for policies that change frequently, fast document queries, natural fit for JSON data |
| **Redis** | Sub-millisecond responses for sessions - can't hit PostgreSQL for every API call authentication |
| **Keycloak** | Security is complex - OAuth2, SAML, MFA, social login - battle-tested, don't reinvent the wheel |
| **Flowable** | Visual workflow design, task assignment, SLA tracking, audit trails - all built-in |
| **MinIO** | Large files don't belong in databases, S3-compatible means easy migration to AWS S3 later |
| **RabbitMQ** | Decouple services - if email service is down, loan processing still works, retry later |

---

## Container Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Flowable UI** | http://localhost:8086/flowable-ui | `admin` / `admin` |
| **Flowable REST** | http://localhost:8085/flowable-rest | `admin` / `admin` |
| **Keycloak Admin** | http://localhost:8180 | `admin` / `admin` |
| **MinIO Console** | http://localhost:9001 | `loanflow` / `loanflow_secret` |
| **RabbitMQ Management** | http://localhost:15672 | `loanflow` / `loanflow_secret` |
| **PostgreSQL** | localhost:5432 | `loanflow` / `loanflow_secret` |
| **MongoDB** | localhost:27017 | `loanflow` / `loanflow_secret` |
| **Redis** | localhost:6379 | password: `loanflow_secret` |

---

## Quick Start

```bash
# Start all containers
cd /path/to/loanflow
docker-compose -f docker/docker-compose.yml up -d

# Check status
docker ps

# View logs
docker-compose -f docker/docker-compose.yml logs -f

# Stop all
docker-compose -f docker/docker-compose.yml down

# Reset everything (removes data)
docker-compose -f docker/docker-compose.yml down -v
```
