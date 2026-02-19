# Sprint 2: Loan & Document Management UI

## Sprint Overview
| Field | Value |
|-------|-------|
| Sprint Number | 2 |
| Duration | 2026-02-19 to 2026-03-05 |
| Status | 🔄 IN PROGRESS |
| GitHub Milestone | [Sprint 2](https://github.com/javbond/loanflow/milestone/5) |

---

## Sprint Goal
> Complete Loan Application and Document Management Angular UI with full E2E testing.

---

## Committed User Stories

| Issue | Title | Points | Assignee | Status |
|-------|-------|--------|----------|--------|
| #11 | [EPIC-002] Loan Application Management | - | - | 🔄 Parent |
| #14 | [US-005] Loan Application Angular UI | 8 | Claude | 🔄 In Progress |
| #15 | [US-006] Document Management Angular UI | 5 | Claude | ⏳ Pending |

**Total Committed: 13 story points**

---

## Task Breakdown

### US-005: Loan Application Angular UI (#14)

| Task # | Description | Est. | Status |
|--------|-------------|------|--------|
| T1 | Create loan models (loan.model.ts) | 2h | ✅ Done |
| T2 | Create loan service (loan.service.ts) | 2h | ✅ Done |
| T3 | Build loan-list component | 3h | 🔄 In Progress |
| T4 | Build loan-form component | 4h | ⏳ Pending |
| T5 | Build loan-detail component | 3h | ⏳ Pending |
| T6 | Add routing & navigation | 1h | ⏳ Pending |
| T7 | UAT Testing & bug fixes | 3h | ⏳ Pending |

### US-006: Document Management Angular UI (#15)

| Task # | Description | Est. | Status |
|--------|-------------|------|--------|
| T1 | Create document models | 2h | ⏳ Pending |
| T2 | Create document service | 2h | ⏳ Pending |
| T3 | Build document-upload component | 4h | ⏳ Pending |
| T4 | Build document-list component | 3h | ⏳ Pending |
| T5 | Build document-viewer component | 3h | ⏳ Pending |
| T6 | UAT Testing & bug fixes | 2h | ⏳ Pending |

---

## Sprint Calendar

```
Week 1 (Feb 19-23):
├── Day 1 (Wed): Sprint Planning ✅
├── Day 2 (Thu): Loan UI - List Component
├── Day 3 (Fri): Loan UI - Form Component
├── Day 4 (Sat): Loan UI - Detail Component
├── Day 5 (Sun): Loan UI - Testing

Week 2 (Feb 24-28):
├── Day 1 (Mon): Document UI - Models & Service
├── Day 2 (Tue): Document UI - Upload Component
├── Day 3 (Wed): Document UI - List & Viewer
├── Day 4 (Thu): Full UAT Testing
├── Day 5 (Fri): Sprint Review & Retro
```

---

## Dependencies & Risks

### Dependencies
- [x] loan-service running on port 8081
- [x] document-service running on port 8083
- [x] customer-service running on port 8082 (for customer lookup)
- [x] PostgreSQL database running
- [x] MinIO running (for document storage)

### Risks
| Risk | Mitigation |
|------|------------|
| API contract mismatch | Review DTOs before coding |
| File upload issues | Test with MinIO early |
| Context loss between sessions | Use CLAUDE.md |

---

## Definition of Done

- [ ] All components functional
- [ ] Manual UAT testing passed
- [ ] GitHub issues updated and closed
- [ ] Sprint review document created
- [ ] CLAUDE.md updated with status

---

## GitHub Integration

### Milestone
- **Name:** Sprint 2
- **Due:** 2026-03-05
- **URL:** https://github.com/javbond/loanflow/milestone/5

### Linked Issues
- #11 - EPIC-002 (Parent)
- #14 - US-005 Loan UI
- #15 - US-006 Document UI

---

## Approval

- [x] Sprint plan reviewed
- [x] GitHub milestone created
- [x] Issues assigned to milestone
- [ ] PO approval received
- [ ] Development started
