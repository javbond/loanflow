# Sprint 6 Plan — Policy Evaluation + Task Assignment

## Sprint Details

| Field | Value |
|-------|-------|
| **Sprint** | 6 |
| **Duration** | 2026-03-06 to 2026-03-20 (2 weeks) |
| **Milestone** | [Sprint 6](https://github.com/javbond/loanflow/milestone/9) |
| **Sprint Goal** | Complete M2 (Policy Engine Live) + Production-grade workflow assignment |
| **Velocity Target** | 16 story points |

---

## Sprint Backlog

| Issue | Story | Points | Priority | Epic |
|-------|-------|--------|----------|------|
| #37 | [US-010] Policy Evaluation Engine | 8 | P1 | EPIC-003 |
| #38 | [US-011] Pre-built Policy Templates | 3 | P2 | EPIC-003 |
| #39 | [US-014] Task Assignment & Escalation | 5 | P1 | EPIC-004 |

**Total**: 16 story points

---

## User Stories Detail

### US-010: Policy Evaluation Engine (8 pts)

**Goal**: Policies exist (CRUD from US-008) but can't evaluate loan applications yet. Build the engine that takes a loan context and runs it against active policies.

**Tasks**:
- [ ] Condition evaluator service (GREATER_THAN, LESS_THAN, EQUALS, IN, BETWEEN, CONTAINS, etc.)
- [ ] Support nested AND/OR condition groups
- [ ] Action executor framework (AUTO_APPROVE, FLAG_REVIEW, REJECT, SET_INTEREST_RATE, etc.)
- [ ] Policy priority resolver (handle conflicting policies)
- [ ] Policy evaluation logging (audit trail)
- [ ] Redis caching for active policies
- [ ] REST API: `POST /api/v1/policies/evaluate`
- [ ] TDD unit tests (target: 15+ new tests)

**Dependencies**: US-008 (Policy Data Model) ✅

---

### US-011: Pre-built Policy Templates (3 pts)

**Goal**: Seed data for UAT testing — realistic policy templates that can be activated immediately.

**Tasks**:
- [ ] Personal Loan template (CIBIL >= 650, age 21-60, income >= 25K)
- [ ] Home Loan template (CIBIL >= 700, age 21-65, income >= 40K, LTV rules)
- [ ] KCC template (land ownership, crop type, district rules)
- [ ] MongoDB DataInitializer bean
- [ ] Templates in DRAFT status, version 1.0.0

**Dependencies**: US-008 (Policy Data Model) ✅, US-010 (Evaluation Engine)

---

### US-014: Task Assignment & Escalation (5 pts)

**Goal**: Auto-assign workflow tasks based on workload/rules and escalate on SLA breach.

**Tasks**:
- [ ] Round-robin assignment TaskListener
- [ ] Workload-based assignment (fewest active tasks)
- [ ] SLA timer boundary events in BPMN
- [ ] Escalation subprocess (reassign to supervisor)
- [ ] Assignment rules configuration
- [ ] Officer workload REST API endpoint
- [ ] TDD unit tests (target: 10+ new tests)

**Dependencies**: US-012 (Flowable BPMN) ✅, US-013 (Task Inbox UI) ✅

---

## Execution Plan

| Week | Focus |
|------|-------|
| Week 1 (Days 1-5) | US-010: Policy Evaluation Engine core + tests |
| Week 1 (Days 4-5) | US-011: Policy Templates (parallel, smaller) |
| Week 2 (Days 6-10) | US-014: Task Assignment & Escalation |
| Week 2 (Day 10) | Sprint Review + UAT |

---

## Definition of Done

- [ ] Code complete with unit tests (>80% coverage)
- [ ] Manual UAT testing passed
- [ ] GitHub issue closed with completion comment
- [ ] CLAUDE.md updated
- [ ] PRs merged to main

---

## Risks

| Risk | Mitigation |
|------|-----------|
| Policy evaluation complexity (nested AND/OR) | Use recursive evaluator pattern, test edge cases early |
| Flowable timer events in embedded mode | Test timer behavior with Flowable test support |
| Redis cache invalidation on policy update | Use Spring Cache eviction with @CacheEvict on save/delete |
