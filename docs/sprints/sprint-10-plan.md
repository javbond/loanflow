# Sprint 10 Plan - e-KYC + Audit Trail + Notifications (EPIC-007)

## Sprint Details

| Field | Value |
|-------|-------|
| Sprint | 10 |
| Duration | 2026-02-23 to 2026-03-09 (2 weeks) |
| Milestone | [Sprint 10](https://github.com/javbond/loanflow/milestone/13) |
| Sprint Goal | Launch EPIC-007 (Regulatory Compliance) with e-KYC verification, audit trail logging, and event-driven notifications via RabbitMQ — advancing Milestone 3 toward completion |
| Velocity Target | 16 story points |

---

## Sprint Backlog

| Issue | Title | Points | Epic | Priority |
|-------|-------|--------|------|----------|
| #52 | [US-029] e-KYC Integration (UIDAI Aadhaar Verification) | 8 | EPIC-007 | P1 |
| #53 | [US-030] Audit Trail & Activity Logging | 5 | EPIC-007 | P1 |
| #54 | [US-031] Event-Driven Notifications (RabbitMQ) | 3 | EPIC-007 | P1 |

**Total**: 16 story points

---

## User Stories Detail

### US-029: e-KYC Integration — UIDAI Aadhaar Verification (8 points) - #52

**As a** loan officer, **I want to** verify applicant identity via UIDAI e-KYC (Aadhaar OTP-based), **so that** KYC compliance is met digitally without physical document verification.

#### Architecture Decision
- **UIDAI API**: Mock/stub in dev environment (realistic response payloads), configurable toggle for production
- **OTP Flow**: Initiate → OTP sent to Aadhaar-linked mobile → Verify OTP → Get e-KYC data (name, DOB, address, photo)
- **CKYC Submission**: After successful e-KYC, submit to Central KYC Registry (mock)
- **Profile-based**: `UidaiEkycService` (uat/prod) + `MockEkycService` (dev/test) — same pattern as OCR and virus scanning

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | UIDAI API client | REST client with retry logic, OTP initiate + verify endpoints | 3h |
| T2 | e-KYC response parser | Parse demographic data: name, DOB, gender, address, photo | 2h |
| T3 | KYC status tracker | `KycVerification` entity in customer-service, status tracking (PENDING → OTP_SENT → VERIFIED → FAILED) | 3h |
| T4 | CKYC submission client | Submit verified KYC to Central KYC Registry (mock) | 2h |
| T5 | e-KYC Angular component | OTP initiation form, verification flow, KYC status display with Aadhaar masking | 3h |
| T6 | Customer-service endpoints | `POST /api/v1/customers/{id}/ekyc/initiate`, `POST .../verify`, `GET .../kyc-status` | 2h |
| T7 | Flowable workflow integration | Service task to check KYC status before underwriting | 2h |
| T8 | TDD unit tests | 12+ tests: client, parser, status tracker, mock service, endpoints | 3h |

#### Dependencies
- customer-service ✅
- Keycloak auth ✅
- Flowable workflow ✅

---

### US-030: Audit Trail & Activity Logging (5 points) - #53

**As a** compliance officer, **I want to** view a complete audit trail of all actions taken on a loan application, **so that** regulatory audits can be satisfied with full traceability.

#### Architecture Decision
- **Spring AOP interceptor**: `@Auditable` annotation on service methods, captures before/after state
- **MongoDB audit collection**: `audit_events` in document-service MongoDB (not Elasticsearch yet — defer full search to Sprint 11)
- **Event types**: APPLICATION_CREATED, STATUS_CHANGED, DOCUMENT_UPLOADED, DOCUMENT_VERIFIED, KYC_VERIFIED, DECISION_MADE, TASK_ASSIGNED, TASK_COMPLETED, APPROVAL_GRANTED
- **Audit viewer UI**: Timeline view on loan-detail, filterable by event type and date range

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | Audit event model | `AuditEvent` MongoDB document: eventType, entityId, entityType, userId, userName, timestamp, beforeState, afterState, ipAddress, details | 2h |
| T2 | Audit interceptor | Spring AOP `@Around` advice for `@Auditable` methods, captures method params + return values | 3h |
| T3 | Audit repository + service | CRUD for audit events, query by applicationId, eventType, dateRange, userId | 2h |
| T4 | REST endpoints | `GET /api/v1/audit/application/{appId}` with filters, `GET /api/v1/audit/user/{userId}` | 1h |
| T5 | Annotate existing services | Add `@Auditable` to key methods in loan-service, document-service, customer-service | 2h |
| T6 | Audit timeline Angular component | Timeline view with event cards, filter chips, expand for details | 3h |
| T7 | TDD unit tests | 8+ tests: interceptor, repository, endpoint, timeline data | 2h |

#### Dependencies
- All services exist ✅
- MongoDB ✅

---

### US-031: Event-Driven Notifications via RabbitMQ (3 points) - #54

**As a** customer, **I want to** receive email/SMS notifications when my loan application status changes, **so that** I stay informed without checking the portal.

#### Architecture Decision
- **RabbitMQ**: PRD-mandated message broker, already in Docker Compose infrastructure
- **notification-service**: New Spring Boot microservice (port 8084), consumes events from RabbitMQ
- **Event publisher**: Loan-service publishes `ApplicationStatusChanged`, `DocumentVerified`, `KycCompleted` events
- **Notification channels**: Email via SMTP (mock in dev), SMS stub (future production integration)
- **Template engine**: Thymeleaf for email templates (already used in loan-service for PDF)

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | notification-service scaffold | New Spring Boot service, RabbitMQ consumer config, port 8084 | 2h |
| T2 | RabbitMQ event publisher | Add `RabbitTemplate` to loan-service, publish status change events | 2h |
| T3 | Email notification handler | Thymeleaf email templates, mock SMTP in dev (MailHog or similar) | 2h |
| T4 | SMS notification stub | Interface + NoOp implementation for future SMS gateway integration | 1h |
| T5 | Notification preferences API | `GET/PUT /api/v1/customers/{id}/notification-preferences` | 1h |
| T6 | TDD unit tests | 5+ tests: publisher, consumer, template rendering, preferences | 2h |

#### Dependencies
- RabbitMQ infrastructure ✅
- loan-service ✅
- customer-service ✅

---

## Execution Plan

### Week 1 (2026-02-23 to 2026-03-01)

| Day | Focus | Stories |
|-----|-------|---------|
| Mon | Sprint kick-off, UIDAI API client + parser | US-029 T1-T2 |
| Tue | KYC status tracker + CKYC submission | US-029 T3-T4 |
| Wed | e-KYC Angular component + REST endpoints | US-029 T5-T6 |
| Thu | Flowable integration + e-KYC tests | US-029 T7-T8 |
| Fri | Audit event model + interceptor | US-030 T1-T2 |

### Week 2 (2026-03-01 to 2026-03-09)

| Day | Focus | Stories |
|-----|-------|---------|
| Mon | Audit repo/service/endpoints + annotate services | US-030 T3-T5 |
| Tue | Audit timeline UI + audit tests | US-030 T6-T7 |
| Wed | notification-service scaffold + RabbitMQ publisher | US-031 T1-T2 |
| Thu | Email handler + SMS stub + preferences + tests | US-031 T3-T6 |
| Fri | Sprint review + UAT | All stories |

---

## Definition of Done

- [ ] All 3 stories implemented with unit tests (>80% coverage)
- [ ] e-KYC mock API returns realistic Aadhaar verification data
- [ ] Audit trail captures all key loan lifecycle events
- [ ] RabbitMQ event flow working: status change → notification-service → email
- [ ] Angular components render correctly for all stories
- [ ] Full E2E workflow: submit loan → e-KYC verify → audit logged → notification sent
- [ ] GitHub issues closed with completion comments
- [ ] Sprint plan and CLAUDE.md updated

---

## Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| UIDAI API format complexity | Medium | Medium | Mock service with realistic payloads; dev/test profile pattern |
| AOP interceptor performance | Medium | Low | Async audit logging; exclude high-frequency read operations |
| RabbitMQ consumer reliability | Medium | Low | Dead letter queue + retry policy |
| notification-service new service overhead | Low | Medium | Minimal scaffold; reuse existing patterns from other services |

---

## Infrastructure Changes

| Component | Change |
|-----------|--------|
| customer-service | Add KycVerification entity, e-KYC endpoints |
| document-service | Add AuditEvent collection, audit interceptor |
| loan-service | Add RabbitMQ publisher, @Auditable annotations |
| notification-service | **NEW** — Spring Boot service on port 8084 |
| Docker Compose | Add MailHog container (mock SMTP) |
| RabbitMQ | Add exchanges/queues for loan events |
