# LoanFlow - Backlog: Epics and User Stories

## Sprint Structure

```
SPRINT DURATION: 2 weeks
TEAM SIZE: 6-8 members (2 FE, 3 BE, 1 QA, 1 DevOps, 0.5 PO)

                              SPRINT CALENDAR

  Day 1 (Monday)
  +-- Sprint Planning (4 hours)
  |   +-- Review Sprint Goal
  |   +-- Select User Stories from Backlog
  |   +-- Break down into Tasks
  |   +-- Estimate and Commit
  +-- Development Begins

  Day 2-9 (Tue-Thu of both weeks)
  +-- Daily Standup (15 min @ 10:00 AM)
  +-- Development + Code Reviews
  +-- Continuous Testing
  +-- Demo to PO (mid-sprint)

  Day 10 (Friday Week 2)
  +-- Sprint Review (2 hours)
  |   +-- Demo completed features
  |   +-- Stakeholder feedback
  |   +-- Acceptance sign-off
  +-- Sprint Retrospective (1.5 hours)
      +-- What went well?
      +-- What needs improvement?
      +-- Action items
```

---

## Epic Format

```
EPIC: [EPIC-ID] [Title]
+----------------------------------------------------------------------+
| Business Value: [Description of business impact]                      |
| Success Metrics: [Measurable outcomes]                                |
| Dependencies: [External dependencies]                                 |
| Estimated Duration: [X sprints]                                       |
+----------------------------------------------------------------------+
```

## User Story Format

```
[US-XXX] As a [ROLE], I want to [ACTION], so that [BENEFIT]
+----------------------------------------------------------------------+
| ACCEPTANCE CRITERIA:                                                  |
| Given [CONTEXT]                                                       |
| When [ACTION]                                                         |
| Then [EXPECTED RESULT]                                                |
+----------------------------------------------------------------------+
| DEFINITION OF DONE (DoD):                                             |
| [ ] Code complete with unit tests (>80% coverage)                     |
| [ ] Code reviewed and approved                                        |
| [ ] Integration tests passing                                         |
| [ ] Security scan passed (no critical/high)                           |
| [ ] API documentation updated                                         |
| [ ] UI/UX reviewed (if applicable)                                    |
| [ ] Deployed to staging environment                                   |
| [ ] PO acceptance                                                     |
+----------------------------------------------------------------------+
```

---

## EPIC-001: Foundation & Infrastructure

```
EPIC-001: Platform Foundation
+-- US-001: Project Setup
|   +-- Task: Create GitHub repository with branch protection
|   +-- Task: Setup Angular 17 project with PrimeNG
|   +-- Task: Setup Spring Boot multi-module Maven project
|   +-- Task: Configure PostgreSQL and MongoDB
|   +-- Task: Setup Docker Compose for local development
|   +-- Task: Configure CI/CD pipeline (GitHub Actions)
|
+-- US-002: Authentication System
|   +-- Task: Setup Keycloak server
|   +-- Task: Configure OAuth2/OIDC in Spring Security
|   +-- Task: Implement Angular auth guard and interceptor
|   +-- Task: Create login/logout UI
|   +-- Task: Implement session management
|
+-- US-003: Role-Based Access Control
    +-- Task: Define roles (LOAN_OFFICER, UNDERWRITER, ADMIN, etc.)
    +-- Task: Implement method-level security
    +-- Task: Create permission management API
    +-- Task: Build admin UI for role assignment
```

---

## EPIC-002: Loan Application Management

```
EPIC-002: Loan Application Core
+-- US-004: Personal Loan Application Form
|   +-- Task: Design multi-step form UI (Angular)
|   +-- Task: Implement form validation (PAN, Aadhaar, Mobile)
|   +-- Task: Create Applicant entity and repository
|   +-- Task: Create LoanApplication aggregate
|   +-- Task: Implement application submission API
|   +-- Task: Add draft save functionality
|
+-- US-005: Co-Applicant Support
|   +-- Task: Extend form for co-applicant capture
|   +-- Task: Implement relationship validation rules
|   +-- Task: Create co-applicant linking logic
|
+-- US-006: Application List & Search
|   +-- Task: Create application list component with filters
|   +-- Task: Implement Elasticsearch integration for search
|   +-- Task: Add export to Excel functionality
|   +-- Task: Build application detail view
|
+-- US-007: Application Status Tracking
    +-- Task: Implement status state machine
    +-- Task: Create status timeline component
    +-- Task: Build customer-facing status page
    +-- Task: Add email/SMS notifications for status change
```

---

## EPIC-003: Policy Engine

```
EPIC-003: Dynamic Policy Engine
+-- US-008: Policy Data Model
|   +-- Task: Design MongoDB schema for policies
|   +-- Task: Implement Policy aggregate (DDD)
|   +-- Task: Create policy CRUD APIs
|   +-- Task: Implement policy versioning
|
+-- US-009: Policy Builder UI
|   +-- Task: Create condition builder component
|   +-- Task: Implement AND/OR grouping UI
|   +-- Task: Build action configuration panel
|   +-- Task: Add policy preview/simulation
|   +-- Task: Create policy activation workflow
|
+-- US-010: Policy Evaluation Engine
|   +-- Task: Implement condition evaluator service
|   +-- Task: Create action executor framework
|   +-- Task: Build policy priority resolver
|   +-- Task: Add policy evaluation logging
|   +-- Task: Implement policy caching (Redis)
|
+-- US-011: Pre-built Policy Templates
    +-- Task: Create Personal Loan eligibility template
    +-- Task: Create Home Loan eligibility template
    +-- Task: Create KCC eligibility template
    +-- Task: Document template customization guide
```

---

## EPIC-004: Workflow & Underwriting

```
EPIC-004: Workflow Engine
+-- US-012: Flowable Integration
|   +-- Task: Setup Flowable embedded in Spring Boot
|   +-- Task: Create loan origination BPMN process
|   +-- Task: Implement service task delegates
|   +-- Task: Configure task assignment rules
|
+-- US-013: Underwriter Workbench
|   +-- Task: Build task inbox component
|   +-- Task: Create application review workspace
|   +-- Task: Implement decision panel (Approve/Reject/Hold)
|   +-- Task: Add condition entry for conditional approvals
|   +-- Task: Build credit memo generator
|
+-- US-014: Task Assignment & Escalation
|   +-- Task: Implement round-robin assignment
|   +-- Task: Create workload-based assignment
|   +-- Task: Build SLA timer configuration
|   +-- Task: Implement escalation workflow
|
+-- US-015: Approval Hierarchy
    +-- Task: Define approval matrix (amount-based)
    +-- Task: Implement delegation of authority
    +-- Task: Create approval workflow routing
```

---

## EPIC-005: Credit Assessment

```
EPIC-005: Credit & Risk Assessment
+-- US-016: Credit Bureau Integration
|   +-- Task: Implement CIBIL API integration
|   +-- Task: Create credit report parser
|   +-- Task: Build credit score display component
|   +-- Task: Implement bureau response caching
|   +-- Task: Add fallback for bureau timeout
|
+-- US-017: Income Verification
|   +-- Task: Implement ITR verification API
|   +-- Task: Create GST return fetcher
|   +-- Task: Build bank statement analyzer
|   +-- Task: Calculate DTI and EMI/NMI ratios
|
+-- US-018: Decision Engine (Drools)
|   +-- Task: Setup Drools with Spring Boot
|   +-- Task: Create eligibility rules (DRL)
|   +-- Task: Implement risk scoring model
|   +-- Task: Build rule hot-reload capability
|   +-- Task: Create rule testing framework
|
+-- US-019: Risk Dashboard
    +-- Task: Create risk score visualization
    +-- Task: Build negative marker alerts
    +-- Task: Implement risk trend analysis
```

---

## EPIC-006: Document Management

```
EPIC-006: Document Management
+-- US-020: Document Upload
|   +-- Task: Create upload component with drag-drop
|   +-- Task: Implement file type validation
|   +-- Task: Setup MinIO storage
|   +-- Task: Create document metadata service
|   +-- Task: Add virus scanning (ClamAV)
|
+-- US-021: Document Verification
|   +-- Task: Implement manual verification workflow
|   +-- Task: Create verification checklist component
|   +-- Task: Build document rejection flow
|   +-- Task: Add re-upload notification
|
+-- US-022: OCR & Data Extraction
|   +-- Task: Integrate Apache Tika
|   +-- Task: Implement PAN card extraction
|   +-- Task: Create Aadhaar data extraction
|   +-- Task: Build extraction review UI
|
+-- US-023: Document Generation
    +-- Task: Create sanction letter template
    +-- Task: Implement loan agreement generator
    +-- Task: Build document signing workflow
    +-- Task: Add digital signature support
```

---

## EPIC-007: Compliance & Integrations

```
EPIC-007: Regulatory Compliance
+-- US-024: e-KYC Integration
|   +-- Task: Implement UIDAI e-KYC API
|   +-- Task: Create OTP-based verification flow
|   +-- Task: Build KYC status tracker
|   +-- Task: Implement CKYC submission
|
+-- US-025: CERSAI Integration
|   +-- Task: Implement CERSAI registration API
|   +-- Task: Create collateral search
|   +-- Task: Build CERSAI status tracker
|   +-- Task: Add modification/satisfaction workflows
|
+-- US-026: Audit Trail
|   +-- Task: Implement audit interceptor
|   +-- Task: Create Elasticsearch audit index
|   +-- Task: Build audit log viewer
|   +-- Task: Add audit report generation
|
+-- US-027: RBI Reporting
    +-- Task: Create PSL report generator
    +-- Task: Implement NPA classification
    +-- Task: Build regulatory submission tracker
```

---

## GitHub Issue Templates

### User Story Template

```markdown
---
name: User Story
about: Create a user story for the backlog
title: '[US-XXX] '
labels: 'user-story'
assignees: ''
---

## User Story
**As a** [role]
**I want to** [action]
**So that** [benefit]

## Acceptance Criteria
- [ ] Given [context], When [action], Then [result]
- [ ] Given [context], When [action], Then [result]

## Technical Notes
<!-- Any technical considerations -->

## Design/Mockups
<!-- Link to Figma or attach screenshots -->

## Dependencies
<!-- List any blocking issues or external dependencies -->

## Definition of Done
- [ ] Code complete with unit tests (>80% coverage)
- [ ] Code reviewed and approved (2 reviewers)
- [ ] Integration tests passing
- [ ] Security scan passed (no critical/high)
- [ ] API documentation updated (if applicable)
- [ ] UI reviewed by UX (if applicable)
- [ ] Deployed to staging
- [ ] PO acceptance
```

### Task Template

```markdown
---
name: Task
about: Create a technical task
title: '[TASK-XXX] '
labels: 'task'
assignees: ''
---

## Parent User Story
Relates to: #[US-XXX]

## Task Description
<!-- Clear description of what needs to be done -->

## Technical Approach
<!-- How will this be implemented? -->

## Files to be Modified
- [ ] `path/to/file1.java`
- [ ] `path/to/file2.ts`

## Test Plan
- [ ] Unit tests for [component]
- [ ] Integration test for [scenario]

## Estimated Effort
- [ ] Small (< 4 hours)
- [ ] Medium (4-8 hours)
- [ ] Large (> 8 hours)
```

---

## GitHub Project Board Organization

### Column: BACKLOG (Future Work)

| Order | Issue | Type | Sprint |
|-------|-------|------|--------|
| 1 | #12 [EPIC-003] Dynamic Policy Engine | Epic | Sprint 3-4 |
| 2 | #11 [EPIC-002] Loan Application Management | Epic | Sprint 2 |

### Column: TO DO (Sprint 1 - Ready to Start)

| Order | Issue | Type | Depends On |
|-------|-------|------|------------|
| 1 | #1 [EPIC-001] Platform Foundation | Epic | - |
| 2 | #2 [US-001] Project Setup & Scaffolding | User Story | - |
| 3 | #5 [TASK-001] Create GitHub repo with branch protection | Task | - |
| 4 | #6 [TASK-002] Setup Spring Boot multi-module Maven | Task | #5 |
| 5 | #7 [TASK-003] Setup Angular 17 project with PrimeNG | Task | #5 |
| 6 | #8 [TASK-004] Configure PostgreSQL and MongoDB | Task | #5 |
| 7 | #9 [TASK-005] Setup Docker Compose for local dev | Task | #8 |
| 8 | #10 [TASK-006] Configure CI/CD pipeline | Task | #6, #7 |
| 9 | #3 [US-002] Authentication System | User Story | #6, #7 |
| 10 | #4 [US-003] Role-Based Access Control | User Story | #3 |

### Column: IN PROGRESS

| Issue | Assignee | Started |
|-------|----------|---------|
| (empty initially) | | |

### Column: IN REVIEW

| Issue | Reviewer | PR Link |
|-------|----------|---------|
| (empty initially) | | |

### Column: DONE

| Issue | Completed Date |
|-------|----------------|
| (empty initially) | |

---

## Recommended Execution Order (Sprint 1)

```
Week 1:
+-- Day 1-2: #5 TASK-001 (GitHub setup) ---------------------------+
+-- Day 2-3: #6 TASK-002 (Spring Boot) <---------------------------+
+-- Day 2-3: #7 TASK-003 (Angular) <-------------------------------+ Parallel
+-- Day 3-4: #8 TASK-004 (PostgreSQL/MongoDB) <--------------------+
+-- Day 4-5: #9 TASK-005 (Docker Compose)

Week 2:
+-- Day 1-2: #10 TASK-006 (CI/CD)
+-- Day 2-4: #3 US-002 (Authentication) ---- Keycloak + Spring Security
+-- Day 4-5: #4 US-003 (RBAC) -------------- Depends on Auth
```
