# Sprint 5 Review - Policy Engine Foundation

## Overview

| Field | Value |
|-------|-------|
| Sprint | 5 |
| Duration | 2026-02-20 to 2026-03-06 |
| Status | ✅ ALL STORIES COMPLETE |
| Milestone | [Sprint 5](https://github.com/javbond/loanflow/milestone/8) |
| Sprint Goal | Implement Dynamic Policy Engine core with MongoDB data model, CRUD APIs, Redis caching, and Flowable BPMN workflow integration |

## Sprint Goal Assessment

**Fully achieved** — Both US-008 (Policy Data Model & CRUD APIs, 8pts) and US-012 (Flowable BPMN Workflow Integration, 5pts) are implemented and tested. 13/13 story points delivered.

---

## Completed Stories

### US-008: Policy Data Model & CRUD APIs (8 points) - #32 ✅ COMPLETE

| Task | Description | Estimate | Actual | Status |
|------|-------------|----------|--------|--------|
| T1 | Create policy-service Spring Boot module | 2h | 2h | ✅ |
| T2 | Design Policy aggregate (DDD pattern) | 3h | 3h | ✅ |
| T3 | Implement Policy repository with MongoDB | 2h | 2h | ✅ |
| T4 | Create Policy CRUD REST APIs | 3h | 3h | ✅ |
| T5 | Implement policy versioning | 2h | 2h | ✅ |
| T6 | Add policy categories & templates | 2h | 2h | ✅ |
| T7 | Integrate Redis for policy caching | 2h | 2h | ✅ |
| T8 | Write unit tests (>80% coverage) | 3h | 4h | ✅ |
| T9 | Policy Builder Angular UI (CRUD list + form) | 4h | 5h | ✅ |

**Total**: ~25h estimated, ~25h actual

#### Acceptance Criteria Verification
- [x] Admin can create policies stored in MongoDB with versioning
- [x] Policy updates create new versions (old version preserved)
- [x] Active policies returned from Redis cache
- [x] Policy categories (eligibility, pricing, credit limit) filterable
- [x] CRUD API secured with ADMIN role

#### Deliverables

**Backend — policy-service (21 Java files)**:
- `PolicyServiceApplication.java` — Spring Boot 3.2 application
- `MongoConfig.java` — Separated `@EnableMongoAuditing` for clean test slicing
- `RedisConfig.java` — Redis template configuration
- `Policy.java` — DDD aggregate root (MongoDB document)
- `PolicyRule.java`, `Condition.java`, `Action.java` — Value objects
- 7 enum classes: `PolicyCategory`, `PolicyStatus`, `LoanType`, `ConditionOperator`, `ActionType`, `LogicalOperator`, `DataType`
- `PolicyRepository.java` — MongoDB repository with custom queries
- `PolicyService.java` + `PolicyServiceImpl.java` — Business logic with Redis caching
- `PolicyController.java` — REST endpoints (12 API endpoints)
- `PolicyRequest.java`, `PolicyResponse.java` — DTOs in common-dto module
- `application.yml` — MongoDB, Redis, Keycloak, RabbitMQ configuration

**Backend Tests — 51 tests, 100% pass rate**:
- `PolicyDomainTest.java` — 21 domain model tests
- `PolicyServiceTest.java` — 15 service layer tests (with Mockito)
- `PolicyControllerTest.java` — 12 controller tests (@WebMvcTest slice)
- 3 nested test class tests

**Frontend — Policy Builder UI (11 files)**:
- `policy.model.ts` — TypeScript interfaces, types, and constants
- `policy.service.ts` — HTTP service (CRUD, lifecycle, search, versioning)
- `policy-list/` — Paginated table with search, category filter, inline actions
- `policy-form/` — Create/edit with dynamic rule builder (FormArray)
- `policy-detail/` — Read-only view with lifecycle actions
- `proxy.conf.json` — Added `/api/v1/policies` proxy to port 8086
- `app.routes.ts` — Added `/policies` route group (ADMIN/SUPERVISOR)
- `app.component.html` — Added "Policies" sidebar navigation

**API Endpoints**:
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/policies` | Create policy |
| GET | `/api/v1/policies` | List policies (paginated) |
| GET | `/api/v1/policies/{id}` | Get policy by ID |
| GET | `/api/v1/policies/code/{code}` | Get by policy code |
| PUT | `/api/v1/policies/{id}` | Update policy |
| DELETE | `/api/v1/policies/{id}` | Delete policy (DRAFT only) |
| POST | `/api/v1/policies/{id}/activate` | Activate policy |
| POST | `/api/v1/policies/{id}/deactivate` | Deactivate policy |
| POST | `/api/v1/policies/{id}/new-version` | Create new version |
| GET | `/api/v1/policies/category/{category}` | List by category |
| GET | `/api/v1/policies/search?keyword=` | Search policies |
| GET | `/api/v1/policies/{id}/versions` | Version history |

---

## US-012: Flowable BPMN Workflow Integration (5 points) - #33 ✅ COMPLETE

| Task | Description | Estimate | Actual | Status |
|------|-------------|----------|--------|--------|
| T1 | Upgrade Flowable 6.8.0→7.1.0, FlowableConfig, app.yml config | 1h | 1h | ✅ |
| T2 | Create BPMN process definition (loan-origination.bpmn20.xml) | 3h | 2h | ✅ |
| T3 | Implement 4 service task delegates + StatusSyncTaskListener | 3h | 3h | ✅ |
| T4 | Create WorkflowService interface and implementation | 2h | 2h | ✅ |
| T5 | Wire WorkflowService into LoanApplicationServiceImpl | 2h | 1h | ✅ |
| T6 | Create Task Inbox API (TaskController + DTOs, 7 endpoints) | 2h | 2h | ✅ |
| T7 | Write unit tests (21 new tests) | 2h | 2h | ✅ |

**Total**: ~15h estimated, ~13h actual

#### Acceptance Criteria Verification
- [x] Loan submission starts Flowable process instance
- [x] Document verification task assigned to LOAN_OFFICER
- [x] Underwriting task assigned to UNDERWRITER
- [x] Tasks visible in underwriter task inbox API
- [x] Task completion advances workflow to next stage

#### Deliverables

**Configuration Changes**:
- `backend/pom.xml` — Flowable version 6.8.0 → 7.1.0 (Spring Boot 3.x compatibility)
- `application.yml` — Flowable config (database-schema-update, history-level, disabled CMMN/DMN/IDM)
- `FlowableConfig.java` — Disables IDM engine (Keycloak handles auth)
- `LoanServiceApplication.java` — Excludes FlowableSecurityAutoConfiguration

**BPMN Process (1 file)**:
- `processes/loan-origination.bpmn20.xml` — Full BPMN 2.0 process with 4 service tasks, 3 user tasks, 2 gateways, approval/rejection/referral paths

**Service Task Delegates (4 files)**:
- `SubmitApplicationDelegate.java` — Transitions SUBMITTED → DOCUMENT_VERIFICATION
- `CreditCheckDelegate.java` — Runs credit check, sets CIBIL score (750 default), transitions to UNDERWRITING
- `ApprovalDelegate.java` — Processes approval with amount and interest rate
- `RejectionDelegate.java` — Processes rejection with reason

**Listener (1 file)**:
- `StatusSyncTaskListener.java` — Syncs LoanApplication.status on user task creation

**WorkflowService (2 files)**:
- `WorkflowService.java` — Interface with 8 methods
- `WorkflowServiceImpl.java` — Implementation using Flowable RuntimeService/TaskService

**Task Inbox API (3 files)**:
- `TaskResponse.java` — DTO with task + loan application context
- `CompleteTaskRequest.java` — DTO with decision (APPROVED/REJECTED/REFERRED)
- `TaskController.java` — 7 REST endpoints secured by role

**Modified Service (1 file)**:
- `LoanApplicationServiceImpl.java` — submit() and createCustomerApplication() start workflow; cancel() terminates workflow

**Tests (2 files, 21 new tests)**:
- `DelegateTests.java` — 8 tests across 4 nested classes (Submit, CreditCheck, Rejection, Approval)
- `WorkflowServiceTest.java` — 13 tests across 6 nested classes (StartProcess, TaskInbox, CompleteTask, Claim, CancelProcess, GetTask)

**Task Inbox API Endpoints**:
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/tasks/inbox` | Tasks for user's roles (candidateGroups) |
| GET | `/api/v1/tasks/my-tasks` | Tasks claimed by current user |
| GET | `/api/v1/tasks/{taskId}` | Single task detail |
| GET | `/api/v1/tasks/application/{id}` | Active task for a loan application |
| POST | `/api/v1/tasks/{taskId}/claim` | Claim a task |
| POST | `/api/v1/tasks/{taskId}/unclaim` | Release task back to pool |
| POST | `/api/v1/tasks/{taskId}/complete` | Complete with decision variables |

---

## Sprint Metrics

| Metric | Value |
|--------|-------|
| Story Points Planned | 13 |
| Story Points Completed | 13 (US-008: 8 + US-012: 5) |
| Story Points Remaining | 0 |
| Completion Rate | 100% |
| Tests Written | 72 (51 policy-service + 21 loan-service workflow) |
| Total Project Tests | 211+ |
| Bugs Found | 0 |
| Files Created | 46+ (35 Java + 11 frontend) |

### Velocity Trend

| Sprint | Planned | Completed | Rate |
|--------|---------|-----------|------|
| Sprint 1 | 16 pts | 16 pts | 100% |
| Sprint 2 | 13 pts | 13 pts | 100% |
| Sprint 3 | 13 pts | 8 pts | 62% |
| Sprint 4 | 5 stories | 5 stories | 100% |
| Sprint 5 | 13 pts | 13 pts | 100% |

---

## UAT Status

**Environment**: All services running and verified
- ✅ 6 Docker containers (postgres, mongodb, minio, redis, rabbitmq, keycloak)
- ✅ 5 Java backend services (customer, loan, document, auth, **policy**)
- ✅ Angular frontend on port 4200 with policy proxy
- ✅ Keycloak auth working (admin@loanflow.com / admin123)
- ✅ API smoke tested (create, activate, list, search all working)
- ✅ Test data: 1 policy in MongoDB (POL-2026-000001, "Personal Loan Eligibility")

**Manual UAT**: Pending user testing via frontend

---

## Demo Summary

### US-008: Policy Data Model & CRUD APIs
1. **Policy CRUD** — Create, read, update, delete policies via REST API
2. **Policy Lifecycle** — DRAFT → ACTIVE → INACTIVE with version management
3. **DDD Aggregate** — Policy as MongoDB aggregate root with rules, conditions, actions
4. **Redis Caching** — Active policies cached for fast retrieval
5. **Angular Policy Builder** — Material Design UI with paginated list, dynamic rule builder form, detail view
6. **Role-Based Access** — Policies restricted to ADMIN/SUPERVISOR roles
7. **51 TDD Tests** — Domain (21), Service (15), Controller (12), nested (3)

### US-012: Flowable BPMN Workflow Integration
8. **BPMN Process** — Loan origination workflow: Submit → Doc Verification → Credit Check → Underwriting → Approve/Reject/Refer
9. **Service Task Delegates** — Auto-executing tasks: submit application, credit check, approval, rejection
10. **User Task Assignment** — LOAN_OFFICER (doc verification), UNDERWRITER (underwriting), SENIOR_UNDERWRITER (referrals)
11. **Task Inbox API** — 7 REST endpoints for task management (inbox, claim, complete, etc.)
12. **Workflow Integration** — Loan submission auto-starts BPMN process; cancel terminates it
13. **21 TDD Tests** — DelegateTests (8), WorkflowServiceTest (13)

---

## Technical Decisions Made

| Decision | Rationale |
|----------|-----------|
| Separated `@EnableMongoAuditing` to `MongoConfig.java` | Prevents `@WebMvcTest` context failures |
| Used `@AutoConfigureMockMvc(addFilters=false)` in tests | Bypasses JWT security for unit test isolation |
| Added fallback top-level `"roles"` claim in SecurityConfig | Makes JWT role extraction resilient to both Keycloak formats |
| Policy-service on port 8086 | Next available port after auth-service (8085) |
| FormArray for dynamic rules | Allows add/remove rules, conditions, actions in form |

---

## Technical Decisions Made (US-012)

| Decision | Rationale |
|----------|-----------|
| Flowable 7.1.0 (not 6.8.0) | Spring Boot 3.2 requires jakarta.* — Flowable 6.x uses javax.* |
| Excluded FlowableSecurityAutoConfiguration | Prevents conflict with Keycloak-based SecurityConfig |
| Disabled IDM engine in FlowableConfig | Authentication/authorization handled by Keycloak, not Flowable IDM |
| candidateGroups without ROLE_ prefix | Spring Security stores ROLE_LOAN_OFFICER; TaskController strips prefix for Flowable matching |
| Process variables passed to BPMN | applicationId, applicationNumber, loanType, requestedAmount used across service tasks |
| StatusSyncTaskListener on user tasks | Keeps LoanApplication.status in sync with BPMN state transitions |
| ApprovalDelegate uses BigDecimal.valueOf() | LoanType.getBaseInterestRate() returns double; needed explicit conversion |

---

## Sign-Off

- [x] US-008 code complete with 51 unit tests
- [x] US-012 code complete with 21 unit tests (48 total loan-service tests)
- [x] Full project build passing (all 9 modules)
- [x] UAT environment fully operational
- [x] API smoke tested end-to-end
- [ ] Manual frontend UAT — pending user testing
- [ ] GitHub issue #32, #33 closure — after UAT approval
- [ ] PR creation & merge — after UAT approval
