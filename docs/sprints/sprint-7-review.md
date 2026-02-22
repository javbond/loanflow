# Sprint 7 Review - Drools Decision Engine + Approval Hierarchy + Risk Dashboard

## Overview

| Field | Value |
|-------|-------|
| Sprint | 7 |
| Duration | 2026-03-20 to 2026-04-03 |
| Status | ✅ ALL STORIES COMPLETE |
| Milestone | [Sprint 7](https://github.com/javbond/loanflow/milestone/10) |
| Sprint Goal | Integrate PRD-mandated Drools engine, add amount-based approval routing, deliver risk analytics dashboard |

## Sprint Goal Assessment

**Fully achieved** — All 3 stories delivered: US-018 (Decision Engine / Drools Integration, 8pts), US-015 (Approval Hierarchy / Amount-Based Matrix, 5pts), US-019 (Risk Dashboard / Score Visualization, 3pts). **16/16 story points delivered.** Additionally, Bug #43 (Task Inbox issues) was discovered and resolved during UAT.

---

## Completed Stories

### US-018: Decision Engine — Drools Integration (8 points) - #40 ✅ COMPLETE

| Task | Description | Status |
|------|-------------|--------|
| T1 | Drools KIE container configuration (`DroolsConfig.java`) | ✅ |
| T2 | Eligibility rules DRL (27 rules: age, income, credit, KYC, LTV, FOIR) | ✅ |
| T3 | Pricing rules DRL (18 rules: base rate, credit premium, employer discount, fees) | ✅ |
| T4 | Fact model POJOs (LoanApplicationFact, ApplicantFact, CreditReportFact, etc.) | ✅ |
| T5 | `DecisionEngineService` — orchestrates Drools KieSession execution | ✅ |
| T6 | `CreditCheckDelegate` — Flowable service task invoking Drools | ✅ |
| T7 | Decision Engine REST API (`/api/v1/decision-engine/evaluate`) | ✅ |
| T8 | Risk tier classification (A/B/C/D based on credit score) | ✅ |
| T9 | Processing fee calculation rules (product-specific, with waiver) | ✅ |
| T10 | TDD unit tests (106 new tests) | ✅ |

**Commit:** `de81e51`

#### Acceptance Criteria Verification
- [x] Drools KIE container loads DRL rules from classpath on startup
- [x] Eligibility rules evaluate age, income, FOIR, LTV, credit score, KYC, employment stability
- [x] Pricing rules compute base rate (RBI repo + spread), apply credit/employer/LTV/tenure adjustments
- [x] CreditCheckDelegate runs automatically after Document Verification completes
- [x] Decision results (CIBIL score, risk category, interest rate, processing fee) persisted to LoanApplication
- [x] Process variables set for underwriting gateway decision
- [x] REST API allows standalone evaluation outside workflow

#### Key Deliverables
- `DroolsConfig.java` — KIE container and session factory bean
- `DecisionEngineService.java` — Orchestrates fact assembly, Drools execution, result extraction
- `CreditCheckDelegate.java` — Flowable JavaDelegate bridging workflow to Drools
- `eligibility-rules.drl` — 27 eligibility rules (age, income, credit, KYC, product-specific)
- `pricing-rules.drl` — 18 pricing rules (base rate, premiums, discounts, fees)
- Fact model: `LoanApplicationFact`, `ApplicantFact`, `EmploymentDetailsFact`, `CreditReportFact`, `EligibilityResultFact`, `PricingResultFact`

---

### US-015: Approval Hierarchy — Amount-Based Matrix (5 points) - #41 ✅ COMPLETE

| Task | Description | Status |
|------|-------------|--------|
| T1 | `ApprovalAuthority` entity + Flyway migration | ✅ |
| T2 | `ApprovalAuthorityRepository` with JPA queries | ✅ |
| T3 | `ApprovalHierarchyResolver` — resolves effective candidate group by loan amount | ✅ |
| T4 | Integrate resolver into `AutoAssignmentTaskListener` | ✅ |
| T5 | Default approval matrix seeded via Flyway (4 tiers) | ✅ |
| T6 | REST API for approval matrix CRUD | ✅ |
| T7 | TDD unit tests | ✅ |

**Commit:** `dd3630f`

#### Acceptance Criteria Verification
- [x] Approval matrix defines amount thresholds per loan type and tier level
- [x] Up to ₹5L → LOAN_OFFICER, ₹5L–₹25L → UNDERWRITER, ₹25L–₹1Cr → SENIOR_UNDERWRITER, above ₹1Cr → BRANCH_MANAGER
- [x] `AutoAssignmentTaskListener` dynamically overrides BPMN candidate group based on loan amount
- [x] Fallback to BPMN-defined group when no hierarchy match
- [x] Matrix is database-driven and manageable via REST API

#### Key Deliverables
- `ApprovalAuthority.java` — JPA entity with tier level, amount thresholds, role group
- `ApprovalAuthorityRepository.java` — Queries for loan-type-specific and fallback matrix
- `ApprovalHierarchyResolver.java` — Resolves effective candidate group for underwriting tasks
- `AutoAssignmentTaskListener.java` — Updated to invoke hierarchy resolver before assignment
- Flyway migration seeding default 4-tier approval matrix

---

### US-019: Risk Dashboard — Score Visualization & Alerts (3 points) - #42 ✅ COMPLETE

| Task | Description | Status |
|------|-------------|--------|
| T1 | `RiskAnalyticsService` — aggregation queries for risk metrics | ✅ |
| T2 | `RiskAnalyticsController` — REST endpoints for dashboard data | ✅ |
| T3 | Risk Dashboard Angular component with PrimeNG charts | ✅ |
| T4 | CIBIL score distribution chart | ✅ |
| T5 | Risk tier breakdown visualization | ✅ |
| T6 | Loan status pipeline chart | ✅ |
| T7 | Navigation and routing integration | ✅ |

**Commit:** `dd57ceb`

#### Acceptance Criteria Verification
- [x] Dashboard shows CIBIL score distribution across applications
- [x] Risk tier breakdown (LOW/MEDIUM/HIGH) with color coding
- [x] Loan status pipeline visualization
- [x] Real-time data from `RiskAnalyticsService` aggregation queries
- [x] Accessible from staff sidebar navigation

#### Key Deliverables
- `RiskAnalyticsService.java` — Aggregation logic for risk metrics
- `RiskAnalyticsController.java` — REST endpoints `/api/v1/risk-analytics/*`
- `risk-dashboard.component.ts/html/scss` — Angular dashboard with PrimeNG charts

---

## Additional Work (UAT & Bug Fixes)

| Item | Description | Commit |
|------|-------------|--------|
| Bug #43 — Keycloak UUID sync | Added explicit `"id"` fields to all 7 users in `realm-export.json` matching `application.yml` officer roster | `02c1faf` |
| Bug #43 — Claim idempotency | Made `claimTask()` check existing assignee: same-user → no-op, different-user → 409, unassigned → claim | `02c1faf` |
| Bug #43 — Interest rate visibility | Added Interest Rate to Credit Info tab and Credit Memo (was hidden behind `@if (loan.approvedAmount)`) | `02c1faf` |
| Bug #43 — Inbox UX | Conditional Claim/View button based on `task.assignee`, smart row click handler | `02c1faf` |
| Bug #43 — Exception handler | Added `IllegalStateException` → HTTP 409 Conflict in `GlobalExceptionHandler` | `02c1faf` |

---

## Sprint Metrics

| Metric | Value |
|--------|-------|
| **Story Points Planned** | 16 |
| **Story Points Delivered** | 16 |
| **Velocity** | 16 pts/sprint (3rd consecutive sprint at 16) |
| **Commits** | 4 feature + 1 bug fix = 5 |
| **Files Changed** | 59 files, +6,958 lines |
| **New Tests (loan-service)** | 106 (from 61 → 167) |
| **Total Project Tests** | 305+ |
| **UAT Result** | ✅ Full E2E workflow verified |
| **Frontend UAT** | ✅ All roles tested |

---

## UAT Summary

### End-to-End Workflow Verified
```
Customer submits loan → DRAFT → SUBMITTED
  → Document Verification (auto-assigned to officer via round-robin)
  → Complete doc verification
  → Credit Check (Drools auto: CIBIL score, risk category, interest rate, processing fee)
  → Underwriting Review (auto-assigned via approval hierarchy based on amount)
  → Decision: APPROVE (with approved amount + rate) / REJECT / REFER
  → If REFERRED → Senior Review (SENIOR_UNDERWRITER or BRANCH_MANAGER per matrix)
  → Final: APPROVED or REJECTED end state
```

### Drools Decision Engine Verification
- Credit score evaluation: scores < 550 → auto-reject, 550-650 → REFER, 650+ → ELIGIBLE
- Interest rate computation: base rate + credit premium + employer discount → final rate
- Processing fee calculation: product-specific with caps and waivers
- Risk tier classification: A (750+), B (700-749), C (650-699), D (550-649)

### Approval Hierarchy Verification
- ₹5L loan → routed to LOAN_OFFICER
- ₹25L loan → routed to UNDERWRITER
- ₹80L loan → routed to SENIOR_UNDERWRITER
- ₹1.5Cr loan → routed to BRANCH_MANAGER

### Bug #43 UAT Verification
- 2 loan applications submitted sequentially — both visible in Task Inbox
- Auto-assigned tasks show "View" button (not "Claim")
- No "already claimed" errors
- Interest Rate visible in Credit Info tab after Drools runs

---

## Known Issues

| Issue | Severity | Status |
|-------|----------|--------|
| CSS budget warning on `risk-dashboard.component.css` (exceeds 4KB) | Low | Cosmetic — only affects production build warning |
| customer-service `UnnecessaryStubbingException` in 4 tests | Low | Pre-existing from Sprint 1, doesn't affect functionality |

---

## Milestone Progress

### M3: Integration Complete — 🔄 IN PROGRESS

| Criteria | Status |
|----------|--------|
| Drools decision engine evaluates rules | ✅ (Sprint 7) |
| Amount-based approval routing | ✅ (Sprint 7) |
| Risk analytics dashboard | ✅ (Sprint 7) |
| CIBIL credit bureau integration | ⏳ Sprint 8 |
| Income verification (ITR, GST) | ⏳ Sprint 8 |
| Enhanced document upload | ⏳ Sprint 8 |
| e-KYC integration (UIDAI) | ⏳ Sprint 9 |
| Email/SMS notifications | ⏳ Future |
