# Sprint 7 Plan — Drools Decision Engine + Approval Hierarchy + Risk Dashboard

## Sprint Details

| Field | Value |
|-------|-------|
| **Sprint** | 7 |
| **Duration** | 2026-03-20 to 2026-04-03 (2 weeks) |
| **Milestone** | [Sprint 7](https://github.com/javbond/loanflow/milestone/10) |
| **Sprint Goal** | Integrate PRD-mandated Drools engine, add amount-based approval routing, deliver risk analytics dashboard |
| **Velocity Target** | 16 story points |

---

## Sprint Backlog

| Issue | Story | Points | Priority | Epic |
|-------|-------|--------|----------|------|
| #40 | [US-018] Decision Engine (Drools) | 8 | P1 | EPIC-005 |
| #41 | [US-015] Approval Hierarchy | 5 | P1 | EPIC-004 |
| #42 | [US-019] Risk Dashboard | 3 | P2 | EPIC-005 |

**Total**: 16 story points

---

## User Stories Detail

### US-018: Decision Engine — Drools Integration (8 pts)

**Goal**: PRD mandates Drools DRL for the Decision Engine. Complete DRL rule files already exist in `docs/rules/`. Integrate Drools with Spring Boot, wire into the Flowable `creditCheck` service task, and enable rule hot-reload.

**Key Context — Drools vs Policy Engine**:
- **Policy-service** (MongoDB/Redis): UI-configurable business rules — created/edited by admins via the Policy Builder UI without code changes. Best for: eligibility thresholds, simple field conditions, loan type configs.
- **Drools** (DRL files): Code-level computational rules — complex credit decisions with calculations (DTI ratios, EMI/NMI, risk scoring models, multi-variable formulas). Best for: credit assessment, pricing algorithms, risk tier classification.
- Both coexist: Policies run first (quick eligibility check), Drools runs second (deep credit analysis in the BPMN `creditCheck` task).

**Tasks**:
- [ ] T1: Add `drools-spring-boot-starter` to loan-service pom.xml (1h)
- [ ] T2: Create `DroolsConfig` — KieContainer/KieSession beans (2h)
- [ ] T3: Move DRL files from `docs/rules/` to `src/main/resources/rules/` (1h)
- [ ] T4: Create `EligibilityFact`, `PricingFact`, `RiskScoreFact` POJOs (2h)
- [ ] T5: Implement `DecisionEngineService` — execute rules with loan facts (3h)
- [ ] T6: Implement `CreditCheckDelegate` (Flowable service task) — calls Drools (2h)
- [ ] T7: Add rule hot-reload capability (KieScanner or scheduled refresh) (2h)
- [ ] T8: Create REST API: `POST /api/v1/decisions/evaluate` (2h)
- [ ] T9: Write TDD unit tests for all rule execution paths (4h)
- [ ] T10: Integration test with Flowable workflow (2h)

**Dependencies**: Existing DRL files (`docs/rules/eligibility-rules.drl`, `docs/rules/pricing-rules.drl`)

---

### US-015: Approval Hierarchy — Amount-Based Matrix (5 pts)

**Goal**: Auto-route underwriting tasks based on loan amount. Currently all tasks go through the same approval flow regardless of amount. Add configurable approval tiers and delegation of authority.

**Default Approval Matrix**:
| Amount Range | Required Approver |
|---|---|
| Up to ₹5,00,000 | LOAN_OFFICER |
| ₹5L – ₹25,00,000 | UNDERWRITER |
| ₹25L – ₹1,00,00,000 | SENIOR_UNDERWRITER |
| Above ₹1Cr | BRANCH_MANAGER |

**Tasks**:
- [ ] T1: Design `ApprovalMatrix` entity (PostgreSQL) — amount ranges, required roles, loan types (2h)
- [ ] T2: Create `ApprovalMatrixRepository` + `ApprovalMatrixService` with CRUD (3h)
- [ ] T3: Create `ApprovalMatrixController` — REST API for matrix management (2h)
- [ ] T4: Implement `DelegationOfAuthority` entity — delegator, delegate, date range, amount limit (2h)
- [ ] T5: Create `DynamicCandidateGroupResolver` — resolves approval groups from matrix (3h)
- [ ] T6: Update BPMN `underwritingReview` task to use dynamic candidate groups (2h)
- [ ] T7: Seed default approval matrix (Flyway migration) (1h)
- [ ] T8: Write TDD unit tests (3h)

**Dependencies**: US-014 (Task Assignment) ✅, Flowable BPMN ✅

---

### US-019: Risk Dashboard — Score Visualization & Alerts (3 pts)

**Goal**: Visual risk analytics dashboard showing CIBIL score distributions, risk tier breakdown, and negative marker alerts.

**Tasks**:
- [ ] T1: Create `RiskDashboardService` — aggregate risk metrics from loan data (3h)
- [ ] T2: Create `RiskDashboardController` — `GET /api/v1/risk/dashboard` (1h)
- [ ] T3: Create `RiskDashboardComponent` in Angular (2h)
- [ ] T4: Implement PrimeNG charts — score distribution bar, risk tier pie (2h)
- [ ] T5: Build negative marker alerts panel (1h)
- [ ] T6: Add route guard and sidebar menu entry (1h)
- [ ] T7: Write backend unit tests (2h)

**Dependencies**: US-018 (Drools risk tier classification)

---

## Execution Plan

| Week | Focus |
|------|-------|
| Week 1 (Days 1-5) | US-018: Drools integration — config, POJOs, service, DRL migration, tests |
| Week 1 (Days 4-5) | US-015: Approval Matrix entity + service (parallel, lower dependency) |
| Week 2 (Days 6-8) | US-015: BPMN integration + delegation of authority |
| Week 2 (Days 8-9) | US-019: Risk Dashboard (backend + frontend) |
| Week 2 (Day 10) | Sprint Review + UAT |

---

## Definition of Done

- [ ] Code complete with unit tests (>80% coverage)
- [ ] Manual UAT testing passed
- [ ] GitHub issue closed with completion comment
- [ ] CLAUDE.md updated
- [ ] Code merged to main

---

## Risks

| Risk | Mitigation |
|------|-----------|
| Drools 9.x compatibility with Spring Boot 3.2 | Verify dependency compatibility early on Day 1 |
| DRL syntax errors in existing rule files | Run Drools compilation as first test |
| KieScanner hot-reload complexity | Fallback to scheduled ClassPathResource refresh |
| Approval matrix complexity with delegation | Keep delegation simple (single-level) for Sprint 7 |
