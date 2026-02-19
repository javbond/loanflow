# Sprint 1: Platform Foundation & Customer Management

## Sprint Overview
| Field | Value |
|-------|-------|
| Sprint Number | 1 |
| Duration | 2026-02-15 to 2026-02-19 |
| Status | ✅ COMPLETED |
| Milestone | [M1: Core Platform](https://github.com/javbond/loanflow/milestone/1) |

---

## Sprint Goal
> Establish project foundation with infrastructure, backend services, and complete Customer Management module end-to-end.

---

## Committed vs Delivered

### Committed User Stories

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #2 | [US-001] Project Setup & Scaffolding | 8 | ✅ Closed |
| #13 | [US-004] Customer Management Module | 8 | ✅ Closed |
| #3 | [US-002] Authentication System | 5 | ⏸️ Backend only |
| #4 | [US-003] Role-Based Access Control | 5 | ⏸️ Backend only |

### Completed Tasks

| Issue | Title | Status |
|-------|-------|--------|
| #5 | [TASK-001] Create GitHub repo with branch protection | ✅ Closed |
| #6 | [TASK-002] Setup Spring Boot multi-module Maven | ✅ Closed |
| #7 | [TASK-003] Setup Angular 17 project | ✅ Closed |
| #8 | [TASK-004] Configure PostgreSQL and MongoDB | ✅ Closed |
| #9 | [TASK-005] Setup Docker Compose | ✅ Closed |
| #10 | [TASK-006] Configure CI/CD pipeline | ✅ Closed |

---

## Metrics

| Metric | Value |
|--------|-------|
| Story Points Committed | 26 |
| Story Points Completed | 16 |
| Velocity | 16 pts |
| Completion Rate | 62% |
| Bugs Found | 4 (all fixed) |
| Test Coverage | 121 TDD tests |

### Backend Services Delivered

| Service | Tests | Status |
|---------|-------|--------|
| customer-service | 45 | ✅ Complete |
| loan-service | 27 | ✅ Complete |
| document-service | 49 | ✅ Complete |

### Frontend Delivered

| Module | Components | Status |
|--------|------------|--------|
| Customer | List, Form, Detail | ✅ Complete |

---

## Bugs Fixed During Sprint

1. **Mobile field mismatch** - Backend `mobileNumber` vs Frontend `mobile`
2. **Country code** - DB expected `IN`, UI sent `India`
3. **KYC verification** - Wrong HTTP method (PUT vs POST)
4. **PAN/Aadhaar edit** - Masked values in edit form

---

## Carryover to Sprint 2

| Issue | Title | Reason |
|-------|-------|--------|
| #3 | [US-002] Authentication System | Frontend pending |
| #4 | [US-003] Role-Based Access Control | Frontend pending |

---

## Retrospective Notes

### 😊 What Went Well
- TDD approach with 121 tests gave confidence
- Backend services completed faster than expected
- UAT testing caught integration issues early

### 😞 What Didn't Go Well
- Field naming mismatches between FE/BE
- Auth/RBAC frontend not completed
- Some context lost between sessions

### 💡 Improvements for Sprint 2
- Use CLAUDE.md for session continuity
- Create `.claudeignore` to reduce token usage
- Document sprint plans in `docs/sprints/`

---

## Sign-off

- [x] Sprint Review conducted
- [x] Retrospective completed
- [x] Carryover items identified
- [x] Sprint 2 planned
