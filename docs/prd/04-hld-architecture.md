# LoanFlow - High-Level Design (HLD)

## System Architecture

```
                              LOAN ORIGINATION SYSTEM
                              HIGH-LEVEL ARCHITECTURE

                                    +-------------+
                                    |   CDN       |
                                    | (CloudFront)|
                                    +------+------+
                                           |
+------------------------------------------+------------------------------------------+
|                              PRESENTATION LAYER                                      |
|  +--------------+  +--------------+  +--------------+  +----------------------+      |
|  | Customer     |  | Branch       |  | Underwriter  |  | Admin/Compliance     |      |
|  | Portal       |  | Portal       |  | Workbench    |  | Dashboard            |      |
|  | (Angular)    |  | (Angular)    |  | (Angular)    |  | (Angular)            |      |
|  +--------------+  +--------------+  +--------------+  +----------------------+      |
+------------------------------------------+------------------------------------------+
                                           |
                                    +------+------+
                                    | API Gateway |
                                    | (Kong/AWS)  |
                                    | + WAF       |
                                    +------+------+
                                           |
+------------------------------------------+------------------------------------------+
|                              SERVICE LAYER (Spring Boot Microservices)               |
|                                                                                      |
|  +-------------+ +-------------+ +-------------+ +-------------+ +------------+      |
|  | Identity    | | Application | | Policy      | | Workflow    | | Decision   |      |
|  | Service     | | Service     | | Engine      | | Service     | | Engine     |      |
|  |             | |             | |             | | (Flowable)  | | (Drools)   |      |
|  +-------------+ +-------------+ +-------------+ +-------------+ +------------+      |
|                                                                                      |
|  +-------------+ +-------------+ +-------------+ +-------------+ +------------+      |
|  | Document    | | Notification| | Integration | | Compliance  | | Reporting  |      |
|  | Service     | | Service     | | Hub         | | Service     | | Service    |      |
|  |             | |             | |             | |             | |            |      |
|  +-------------+ +-------------+ +-------------+ +-------------+ +------------+      |
|                                                                                      |
+------------------------------------------+------------------------------------------+
                                           |
                              +------------+------------+
                              |     MESSAGE BUS         |
                              |     (Apache Kafka)      |
                              +------------+------------+
                                           |
+------------------------------------------+------------------------------------------+
|                              DATA LAYER                                              |
|                                                                                      |
|  +-------------+  +-------------+  +-------------+  +-------------------------+      |
|  | PostgreSQL  |  | MongoDB     |  | Redis       |  | Elasticsearch           |      |
|  | (Primary)   |  | (Policies/  |  | (Cache/     |  | (Search/Audit)          |      |
|  |             |  |  Workflows) |  |  Sessions)  |  |                         |      |
|  +-------------+  +-------------+  +-------------+  +-------------------------+      |
|                                                                                      |
|  +-----------------------------------------------------------------------------+    |
|  |                    MinIO (S3-Compatible Document Storage)                    |    |
|  +-----------------------------------------------------------------------------+    |
+-------------------------------------------------------------------------------------+

+-------------------------------------------------------------------------------------+
|                              EXTERNAL INTEGRATIONS                                   |
|                                                                                      |
|  +-------------+  +-------------+  +-------------+  +-------------------------+      |
|  | CIBIL       |  | UIDAI       |  | CERSAI      |  | Bank CBS               |      |
|  | Experian    |  | (Aadhaar    |  |             |  | (Core Banking)         |      |
|  | Equifax     |  |  e-KYC)     |  |             |  |                         |      |
|  | CRIF        |  |             |  |             |  |                         |      |
|  +-------------+  +-------------+  +-------------+  +-------------------------+      |
|                                                                                      |
|  +-------------+  +-------------+  +-------------+  +-------------------------+      |
|  | GST Portal  |  | ITR/Form 26AS| | NSDL (PAN)  |  | DigiLocker             |      |
|  |             |  |             |  |             |  |                         |      |
|  +-------------+  +-------------+  +-------------+  +-------------------------+      |
+-------------------------------------------------------------------------------------+
```

---

## Bounded Contexts (DDD Strategic Design)

```
                         BOUNDED CONTEXTS (DDD)

+-------------------------------------------------------------------------+
|                              CORE DOMAIN                                 |
|                    (Competitive Advantage - Build In-House)              |
+-------------------------------------------------------------------------+
|                                                                          |
|  +---------------------+    +---------------------+    +-----------------+
|  |  LOAN APPLICATION   |    |   POLICY ENGINE     |    |  CREDIT         |
|  |     CONTEXT         |    |     CONTEXT         |    |  ASSESSMENT     |
|  |                     |    |                     |    |  CONTEXT        |
|  | - Application       |    | - Policy            |    | - CreditReport  |
|  | - Applicant         |    | - Condition         |    | - RiskScore     |
|  | - Collateral        |    | - Action            |    | - Scorecard     |
|  | - LoanOffer         |    | - PolicyVersion     |    | - Assessment    |
|  +---------------------+    +---------------------+    +-----------------+
|           |                          |                          |        |
|           +------------------------------------------+----------+        |
|                                      |                                   |
|                            [Shared Kernel: Money, DateRange]             |
|                                                                          |
+--------------------------------------------------------------------------+

+--------------------------------------------------------------------------+
|                           SUPPORTING DOMAIN                               |
|                      (Necessary but not differentiating)                  |
+--------------------------------------------------------------------------+
|                                                                           |
|  +---------------------+    +---------------------+    +------------------+
|  |  WORKFLOW           |    |   DOCUMENT          |    |  COMPLIANCE      |
|  |  CONTEXT            |    |   CONTEXT           |    |  CONTEXT         |
|  |                     |    |                     |    |                  |
|  | - ProcessInstance   |    | - Document          |    | - KYCRecord      |
|  | - Task              |    | - DocumentType      |    | - CERSAIRecord   |
|  | - Assignment        |    | - Verification      |    | - AuditLog       |
|  +---------------------+    +---------------------+    +------------------+
|                                                                           |
+---------------------------------------------------------------------------+

+---------------------------------------------------------------------------+
|                           GENERIC DOMAIN                                   |
|                        (Commoditized - Use Libraries)                      |
+---------------------------------------------------------------------------+
|                                                                            |
|  +---------------------+    +---------------------+    +-------------------+
|  |  IDENTITY           |    |   NOTIFICATION      |    |  REPORTING        |
|  |  CONTEXT            |    |   CONTEXT           |    |  CONTEXT          |
|  |                     |    |                     |    |                   |
|  | - User (Keycloak)   |    | - Template          |    | - Report          |
|  | - Role              |    | - Channel           |    | - Dashboard       |
|  | - Permission        |    | - NotificationLog   |    | - Export          |
|  +---------------------+    +---------------------+    +-------------------+
|                                                                            |
+----------------------------------------------------------------------------+
```

---

## Context Map

```
                            CONTEXT MAP

                    +---------------------+
                    |   LOAN APPLICATION  |
                    |      (Upstream)     |
                    +----------+----------+
                               |
           +-------------------+-------------------+
           |                   |                   |
           v                   v                   v
+------------------+ +------------------+ +------------------+
|  POLICY ENGINE   | |    WORKFLOW      | | CREDIT ASSESSMENT|
|   (Downstream)   | |   (Downstream)   | |   (Downstream)   |
|                  | |                  | |                  |
| Conformist       | | Customer-Supplier| | Anti-Corruption  |
| (follows app     | | (negotiated      | | Layer (external  |
|  structure)      | |  contract)       | |  bureaus)        |
+------------------+ +------------------+ +------------------+
                               |
                               v
                    +------------------+
                    |   COMPLIANCE     |
                    |   (Downstream)   |
                    |                  |
                    | Open Host Service|
                    | (published API)  |
                    +------------------+
```

---

## Database Schema Design

### PostgreSQL Tables

| Schema | Tables |
|--------|--------|
| `identity` | users, roles, permissions, user_roles |
| `application` | loan_applications, applicants, co_applicants, employment, addresses, collaterals, loan_offers |
| `document` | documents, document_types, document_verifications |
| `workflow` | (Flowable tables - auto-generated) |
| `compliance` | kyc_records, cersai_registrations, audit_logs |
| `notification` | notification_templates, notification_logs |

### MongoDB Collections

| Collection | Purpose |
|------------|---------|
| `policies` | Dynamic policy definitions (Entra-style) |
| `policy_versions` | Policy version history |
| `workflow_definitions` | BPMN process definitions |
| `drools_rules` | Decision engine rule packages |
| `credit_reports` | Cached credit bureau responses |

---

## API Specifications

| Service | Base Path | Key Endpoints |
|---------|-----------|---------------|
| Identity | `/api/v1/identity` | /users, /roles, /permissions, /auth |
| Application | `/api/v1/applications` | /loans, /applicants, /collaterals, /offers |
| Policy | `/api/v1/policies` | /policies, /conditions, /evaluate |
| Workflow | `/api/v1/workflow` | /processes, /tasks, /assignments |
| Document | `/api/v1/documents` | /upload, /verify, /generate |
| Decision | `/api/v1/decision` | /evaluate, /scores, /rules |
| Compliance | `/api/v1/compliance` | /kyc, /cersai, /audit |

### Core Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/applications | Create loan application |
| GET | /api/v1/applications/{id} | Get application details |
| PUT | /api/v1/applications/{id}/submit | Submit for processing |
| POST | /api/v1/policies/evaluate | Evaluate policies for application |
| GET | /api/v1/workflow/tasks | Get assigned tasks |
| POST | /api/v1/workflow/tasks/{id}/complete | Complete workflow task |
| POST | /api/v1/documents/upload | Upload document |
| POST | /api/v1/decision/evaluate | Run decision rules |

---

## BPMN Workflow - Loan Origination

```
[Start] -> [Application Capture] -> [Document Upload] ->
[KYC Verification] -> [Credit Bureau Fetch] ->
[Policy Evaluation] -> [Auto-Decision?]
    +-- YES -> [Auto-Approve/Reject] -> [Notification] -> [End]
    +-- NO -> [Manual Underwriting] -> [Approval Matrix] ->
           [Senior Approval?] -> [Disbursement Prep] ->
           [Pre-Disbursement Check] -> [Disburse] -> [End]
```

---

## Repository Structure

```
loanflow/
+-- .github/
|   +-- ISSUE_TEMPLATE/
|   |   +-- bug_report.md
|   |   +-- feature_request.md
|   |   +-- user_story.md
|   |   +-- task.md
|   +-- PULL_REQUEST_TEMPLATE.md
|   +-- workflows/
|   |   +-- ci.yml
|   |   +-- cd-staging.yml
|   |   +-- cd-production.yml
|   |   +-- security-scan.yml
|   |   +-- dependency-check.yml
|   +-- CODEOWNERS
|
+-- docs/
|   +-- architecture/
|   |   +-- HLD.md
|   |   +-- LLD.md
|   |   +-- DDD-context-map.md
|   |   +-- diagrams/
|   +-- api/
|   |   +-- openapi.yaml
|   +-- runbooks/
|   |   +-- deployment.md
|   |   +-- troubleshooting.md
|   |   +-- incident-response.md
|   +-- compliance/
|       +-- security-controls.md
|       +-- rbi-checklist.md
|
+-- backend/
|   +-- api-gateway/
|   +-- identity-service/
|   +-- application-service/
|   +-- policy-engine/
|   +-- workflow-service/
|   +-- decision-engine/
|   +-- document-service/
|   +-- notification-service/
|   +-- integration-hub/
|   +-- compliance-service/
|   +-- reporting-service/
|   +-- common/
|       +-- common-dto/
|       +-- common-security/
|       +-- common-utils/
|
+-- frontend/
|   +-- loanflow-ui/
|       +-- src/
|       |   +-- app/
|       |   |   +-- core/
|       |   |   +-- shared/
|       |   |   +-- features/
|       |   |   |   +-- loan-application/
|       |   |   |   +-- policy-builder/
|       |   |   |   +-- workflow-designer/
|       |   |   |   +-- underwriting/
|       |   |   |   +-- document-mgmt/
|       |   |   |   +-- dashboard/
|       |   |   |   +-- admin/
|       |   |   +-- layouts/
|       |   +-- assets/
|       |   +-- environments/
|       +-- angular.json
|       +-- package.json
|
+-- infrastructure/
|   +-- docker/
|   |   +-- docker-compose.yml
|   |   +-- docker-compose.dev.yml
|   |   +-- Dockerfiles/
|   +-- kubernetes/
|   |   +-- base/
|   |   +-- overlays/
|   |   |   +-- dev/
|   |   |   +-- staging/
|   |   |   +-- production/
|   |   +-- helm-charts/
|   +-- terraform/
|       +-- modules/
|       +-- environments/
|
+-- tests/
|   +-- integration/
|   +-- e2e/
|   +-- performance/
|   +-- security/
|
+-- scripts/
|   +-- setup-local-env.sh
|   +-- run-migrations.sh
|   +-- generate-test-data.sh
|
+-- README.md
+-- CONTRIBUTING.md
+-- CODE_OF_CONDUCT.md
+-- SECURITY.md
+-- LICENSE
```
