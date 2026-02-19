# Sprint 2 Review - Loan & Document Management

**Sprint Duration**: 2026-02-19 to 2026-02-19 (1 day)
**Velocity**: 13 story points
**Status**: ✅ COMPLETED

---

## Sprint Goal
Implement complete Loan Application and Document Management functionality with Angular frontend integration.

---

## Completed Items

### EPIC-002: Loan Application Management (#11)

#### US-005: Loan Application Angular UI (8 points) (#14)
- ✅ Loan List component with pagination and filters
- ✅ Loan Form with customer lookup autocomplete
- ✅ Loan Detail view with full workflow
- ✅ Status transitions (Submit → Approve/Reject/Cancel)
- ✅ Integration with loan-service backend

#### US-006: Document Management Angular UI (5 points) (#15)
- ✅ Document List with status/category filters
- ✅ Document Upload with loan search, drag-drop, progress tracking
- ✅ Document Viewer with verification actions
- ✅ Download with MinIO presigned URLs
- ✅ Integration with document-service backend

---

## Bug Fixes During Sprint

| Issue | Description | Resolution |
|-------|-------------|------------|
| #16 | Document Upload UUID mismatch | Added loan search autocomplete with UUID storage |
| #17 | MinIO credentials mismatch | Updated application.yml credentials |
| #18 | Document Verification 500 | Fixed verifierId to valid UUID format |
| #19 | No document list visible | Added getAll endpoint, changed default route |

---

## Demo Summary

### Loan Management
1. View all loan applications with pagination
2. Create new loan for existing customer
3. View loan details with full information
4. Approve/Reject/Cancel loan applications
5. Filter loans by status and date range

### Document Management
1. View all documents or filter by application
2. Upload documents with loan lookup
3. View document details with preview
4. Verify or reject documents
5. Download documents via presigned URLs

---

## Technical Achievements

### Backend
- loan-service: 27 tests passing
- document-service: 49 tests passing
- MinIO integration working
- UAT profile (security disabled for testing)

### Frontend
- Angular 17 with standalone components
- New control flow syntax (@if, @for)
- Material Design components
- Lazy loading for all feature modules

---

## Sprint Metrics

| Metric | Value |
|--------|-------|
| Story Points Committed | 13 |
| Story Points Completed | 13 |
| Bugs Found | 4 |
| Bugs Fixed | 4 |
| Velocity | 13 points/sprint |

---

## What Went Well
1. TDD approach caught issues early
2. UAT testing revealed integration bugs
3. Bug handling protocol (GitHub Issues first) worked well
4. Quick bug resolution turnaround

## What Could Improve
1. Placeholder values need to be addressed (e.g., currentUserId)
2. Security (currently disabled for UAT)
3. E2E tests would help prevent regressions

---

## Next Sprint (Sprint 3)

### Focus: Security & Authentication
- US-002: Authentication System (JWT, Login/Logout)
- US-003: Role-Based Access Control (RBAC)

### Blocked/Deferred
- None

---

## Sign-Off
- [x] All user stories completed
- [x] All bugs resolved
- [x] GitHub milestone 100% closed
- [x] UAT verification passed
- [x] Documentation updated
