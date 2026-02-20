# Sprint 5 Plan - Policy Engine Foundation

**Sprint Duration**: 2026-02-20 to 2026-03-06 (2 weeks)
**Sprint Goal**: Implement Dynamic Policy Engine core with MongoDB data model, CRUD APIs, Redis caching, and Flowable BPMN workflow integration for loan processing automation.
**Total Story Points**: 13
**Velocity (Average)**: 12.3 points/sprint

---

## Sprint Backlog

### EPIC-003: Dynamic Policy Engine (#12)

#### US-008: Policy Data Model & CRUD APIs (8 points) - #32

**User Story**: As an admin, I want to create and manage loan policies via APIs so that loan rules can be configured without code changes.

| Task | Description | Estimate | Status |
|------|-------------|----------|--------|
| T1 | Create policy-service Spring Boot module (pom.xml, config, MongoDB) | 2h | ✅ |
| T2 | Design Policy aggregate (DDD): Policy → PolicyVersion → PolicyRule → Condition/Action | 3h | ✅ |
| T3 | Implement Policy repository with MongoDB | 2h | ✅ |
| T4 | Create Policy CRUD REST APIs (create, read, update, activate/deactivate) | 3h | ✅ |
| T5 | Implement policy versioning (create new version, rollback) | 2h | ✅ |
| T6 | Add policy categories & templates (eligibility, pricing, credit limit) | 2h | ✅ |
| T7 | Integrate Redis for policy caching | 2h | ✅ |
| T8 | Write unit tests (>80% coverage) — 51 tests passing | 3h | ✅ |
| T9 | Policy Builder Angular UI (basic CRUD list + form + detail) | 4h | ✅ |

**Acceptance Criteria**:
- [x] Admin can create policies stored in MongoDB with versioning
- [x] Policy updates create new versions (old version preserved)
- [x] Active policies returned from Redis cache
- [x] Policy categories (eligibility, pricing, credit limit) filterable
- [x] CRUD API secured with ADMIN role

---

#### US-012: Flowable BPMN Workflow Integration (5 points) - #33

**User Story**: As a system, I want loan applications to follow a BPMN workflow so that processing is automated and auditable.

| Task | Description | Estimate | Status |
|------|-------------|----------|--------|
| T1 | Upgrade Flowable 6.8.0→7.1.0, add config, FlowableConfig.java | 1h | ✅ |
| T2 | Create BPMN process definition (loan-origination.bpmn20.xml) | 3h | ✅ |
| T3 | Implement 4 service task delegates + StatusSyncTaskListener | 3h | ✅ |
| T4 | Create WorkflowService interface and implementation | 2h | ✅ |
| T5 | Wire WorkflowService into LoanApplicationServiceImpl (submit, cancel) | 2h | ✅ |
| T6 | Create Task Inbox API (TaskController + DTOs, 7 endpoints) | 2h | ✅ |
| T7 | Write unit tests (21 new tests — DelegateTests + WorkflowServiceTest) | 2h | ✅ |

**Acceptance Criteria**:
- [x] Loan submission starts Flowable process instance
- [x] Document verification task assigned to LOAN_OFFICER
- [x] Underwriting task assigned to UNDERWRITER
- [x] Tasks visible in underwriter task inbox API
- [x] Task completion advances workflow to next stage

---

## Stretch Goal (if time permits)

#### US-003: RBAC Admin UI (3 points remaining) - #4

- Admin panel for user/role management via Keycloak Admin API
- Simplified scope since Keycloak handles heavy lifting

---

## Technical Architecture

### New: policy-service
```
backend/policy-service/
├── src/main/java/com/loanflow/policy/
│   ├── config/
│   │   ├── MongoConfig.java
│   │   └── RedisConfig.java
│   ├── controller/
│   │   └── PolicyController.java
│   ├── domain/
│   │   ├── aggregate/Policy.java
│   │   ├── entity/PolicyVersion.java
│   │   ├── entity/PolicyRule.java
│   │   ├── valueobject/Condition.java
│   │   ├── valueobject/Action.java
│   │   └── enums/PolicyCategory.java
│   ├── repository/
│   │   └── PolicyRepository.java
│   ├── service/
│   │   ├── PolicyService.java
│   │   └── PolicyCacheService.java
│   └── dto/
│       ├── PolicyRequest.java
│       └── PolicyResponse.java
└── src/main/resources/
    ├── application.yml
    └── application-uat.yml
```

### Enhancement: loan-service (Flowable)
```
backend/loan-service/
├── src/main/java/com/loanflow/loan/
│   ├── workflow/
│   │   ├── LoanOriginationProcess.java
│   │   ├── delegates/
│   │   │   ├── DocumentVerificationDelegate.java
│   │   │   ├── CreditCheckDelegate.java
│   │   │   └── UnderwritingDelegate.java
│   │   └── listeners/
│   │       └── TaskAssignmentListener.java
│   └── controller/
│       └── TaskController.java (new - task inbox)
└── src/main/resources/
    └── processes/
        └── loan-origination.bpmn20.xml
```

### Frontend
```
frontend/loanflow-web/src/app/features/
├── policy/                    (NEW)
│   ├── components/
│   │   ├── policy-list/
│   │   ├── policy-form/
│   │   └── policy-detail/
│   └── services/
│       └── policy.service.ts
```

---

## BPMN Process Definition

```
Start Event
    │
    ▼
[Submit Application] ──── Service Task (auto)
    │
    ▼
[Document Verification] ── User Task (LOAN_OFFICER)
    │
    ▼
[Credit Check] ──────────── Service Task (auto)
    │
    ▼
[Underwriting Review] ──── User Task (UNDERWRITER)
    │
    ├── Approved ──► [Disbursement] ──► End Event
    ├── Rejected ──► [Notify Customer] ──► End Event
    └── Referred ──► [Senior Review] ──► (loop back)
```

---

## Tech Stack (PRD Compliance)

| Component | Technology | PRD Required |
|-----------|-----------|-------------|
| Policy Storage | MongoDB | ✅ Yes |
| Policy Cache | Redis | ✅ Yes |
| Workflow Engine | Flowable BPMN 7.x | ✅ Yes |
| Backend | Spring Boot 3.2 | ✅ Yes |
| Frontend | Angular 17 | ✅ Yes |

---

## Dependencies

- Flowable Spring Boot Starter 7.x
- Spring Data MongoDB (existing in document-service, reuse patterns)
- Spring Data Redis (infrastructure already in Docker Compose)
- MongoDB (already running on port 27017)
- Redis (already running on port 6379)

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Flowable learning curve | Start with simple linear process, extend later |
| MongoDB schema design | Follow DDD aggregate pattern from document-service |
| Redis cache invalidation | Use TTL-based expiry + manual invalidation on update |

---

## Success Metrics

- [ ] policy-service running with >80% test coverage
- [x] Flowable process starts on loan submission
- [x] Task inbox returns assigned tasks by role
- [x] Policy CRUD working end-to-end (API + UI)
