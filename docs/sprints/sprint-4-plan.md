# Sprint 4 Plan - Customer Self-Service Portal (EPIC-004)

## Sprint Details

| Field | Value |
|-------|-------|
| Sprint | 4 |
| Duration | 2026-02-20 |
| Milestone | [Sprint 4](https://github.com/javbond/loanflow/milestone/7) |
| Sprint Goal | Implement a complete Customer Self-Service Portal enabling loan applicants to submit applications, upload documents, track status, and manage their profile |
| Velocity Target | 21 story points |
| Parent Milestone | M2: Feature Complete |

---

## Sprint Backlog

| Issue | Title | Points | Epic | Priority |
|-------|-------|--------|------|----------|
| #26 | [US-024] Customer Loan Application Form | 5 | EPIC-004 | P0 |
| #27 | [US-025] Customer Document Upload | 5 | EPIC-004 | P0 |
| #28 | [US-026] Loan Offer Accept/Reject | 3 | EPIC-004 | P1 |
| #29 | [US-027] Document Download | 3 | EPIC-004 | P1 |
| #30 | [US-028] Customer Dashboard | 5 | EPIC-004 | P1 |

**Total**: 21 story points

---

## User Stories Detail

### US-024: Customer Loan Application Form (5 points) - #26

**As a** customer, **I want** a multi-step loan application form, **so that** I can submit loan applications through the self-service portal.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | Multi-step form with Angular Stepper | Personal details, loan details, employment, review | 4h |
| T2 | Form validation | PAN, Aadhaar, mobile, email format validators | 2h |
| T3 | Customer-specific API endpoint | JWT email extraction, customer-scoped queries | 2h |
| T4 | Draft save and submit | Save as DRAFT, submit workflow | 1h |

### US-025: Customer Document Upload (5 points) - #27

**As a** customer, **I want** to upload documents for my loan application, **so that** required documents are attached for processing.

### US-026: Loan Offer Accept/Reject (3 points) - #28

**As a** customer, **I want** to accept or reject an approved loan offer, **so that** I can confirm the loan terms.

### US-027: Document Download (3 points) - #29

**As a** customer, **I want** to download documents from my loan application, **so that** I can keep records.

### US-028: Customer Dashboard (5 points) - #30

**As a** customer, **I want** a personalized dashboard, **so that** I can see my loans, documents, and status at a glance.

---

## Definition of Done

- [x] All 5 stories complete with unit tests
- [x] Customer portal accessible with customer role JWT
- [x] Multi-step application form working end-to-end
- [x] Document upload/download via MinIO working
