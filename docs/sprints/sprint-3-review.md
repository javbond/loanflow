# Sprint 3 Review - Security & Authentication + Customer Portal

**Sprint Duration**: 2026-02-20 to 2026-03-06 (2 weeks)
**Velocity**: 8 story points (planned 13)
**Status**: ✅ COMPLETED (with carryover)

---

## Sprint Goal
Implement complete authentication and authorization system using Keycloak OAuth2/OIDC, and lay foundation for role-based access control.

**Was the goal achieved?** Partially. Authentication was fully implemented with Keycloak OAuth2/OIDC (exceeding the original JWT-only plan by adopting industry-standard SSO). RBAC backend infrastructure is in place but admin UI for role management was not completed.

---

## Completed Items

### EPIC-001: Platform Foundation (#1)

#### US-002: Authentication System (8 points) - #3 ✅ CLOSED

- ✅ Keycloak OAuth2/OIDC integration (upgraded from standalone JWT per PRD)
- ✅ auth-service Spring Boot module with JWT validation
- ✅ Angular auth service with keycloak-angular integration
- ✅ Login/logout via Keycloak redirect flow
- ✅ JWT token interceptor for API requests
- ✅ AuthGuard for protected routes
- ✅ Role-based route guards (CustomerGuard, StaffGuard)
- ✅ User session management (token storage, refresh)
- ✅ 10 unit tests passing
- ✅ Realm export with 4 pre-configured users (admin, officer, underwriter, customer)

**PR**: #20 - feat(auth): Keycloak OAuth2/OIDC Authentication System (merged 2026-02-19)

---

## Carryover (Incomplete)

#### US-003: Role-Based Access Control (5 points) - #4 🔄 PARTIAL

| Completed | Not Done |
|-----------|----------|
| ✅ Keycloak roles defined (6 roles) | ❌ Admin UI for user/role management |
| ✅ Spring Security role extraction from JWT | ❌ Role assignment API endpoints |
| ✅ @PreAuthorize on all controllers | ❌ Integration tests for RBAC |
| ✅ Customer vs Staff route separation |  |
| ✅ Role-based sidebar navigation |  |

**Reason**: Backend RBAC infrastructure was completed as part of US-002. The remaining admin UI and role management API can be a separate story in a future sprint. Keycloak Admin Console serves as interim role management.

---

## Additional Work (Unplanned but Completed)

### Customer Portal Foundation (Sprint 4 milestone)

During the authentication work, the Customer Portal was developed as a natural extension of role-based routing. The following were completed under Sprint 4 milestone but during Sprint 3 timeframe:

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #26 | [US-024] Customer Loan Application Form | P0 | ✅ Closed |
| #27 | [US-025] Customer Document Upload | P0 | ✅ Closed |
| #28 | [US-026] Loan Offer Accept/Reject | P1 | ✅ Closed |
| #29 | [US-027] Document Download | P1 | ✅ Closed |
| #30 | [US-028] Customer Profile Management | P2 | ✅ Closed |

**PR**: #31 - feat(customer-portal): Complete Customer Self-Service Portal (merged 2026-02-20)

---

## Bug Fixes During Sprint

| Bug | Description | Root Cause | Resolution |
|-----|-------------|------------|------------|
| Keycloak SSL 403 | Token endpoint returning 403 | `sslRequired: "external"` in realm config | Changed to `"none"` for dev/UAT |
| JWT Role Claim Mismatch | Spring Security not extracting roles | Keycloak mapper used `"roles"` instead of `"realm_access.roles"` | Fixed mapper + added fallback in SecurityConfig |
| Missing Email/Profile Claims | JWT missing user info | Missing `email` and `profile` client scopes | Added full scope definitions with protocol mappers |
| 403 on Loan Submission | Customer role not recognized on POST | Combined effect of SSL + role claim issues | Fixed by above three items |

---

## Demo Summary

### Authentication System
1. Login via Keycloak OAuth2/OIDC redirect flow
2. Role-based routing (Customer Portal vs Staff Dashboard)
3. JWT token refresh and session management
4. Logout with token invalidation
5. AuthGuard protecting all routes

### Customer Portal (Bonus)
1. Customer dashboard with role-based sidebar
2. Loan application submission form
3. Application status tracking (My Applications)
4. Document upload for loan applications
5. Document download via presigned URLs
6. Loan offer accept/reject workflow
7. Customer profile view

---

## Technical Achievements

### Backend
- auth-service: 10 tests passing (Keycloak OAuth2 Resource Server)
- common-security module: Shared SecurityConfig with JWT role extraction
- Three-tier role extraction: `realm_access.roles` → `resource_access` → top-level `roles` (fallback)
- @PreAuthorize annotations on all service controllers

### Frontend
- keycloak-angular integration with PKCE (S256)
- Role-based lazy-loaded routes (`/my-portal/*` for customers, `/` for staff)
- Auth interceptor adding Bearer token to all API calls
- Conditional sidebar navigation based on user roles
- Customer portal with 5 feature components

### Infrastructure
- Keycloak realm-export.json with full configuration
- 4 pre-configured test users with distinct roles
- Docker Compose Keycloak service with realm import
- Fixed `sslRequired` permanently for dev/UAT

---

## Sprint Metrics

| Metric | Value |
|--------|-------|
| Story Points Planned | 13 (US-002: 8, US-003: 5) |
| Story Points Completed | 8 (US-002 only) |
| Story Points Carried Over | 5 (US-003 partial - admin UI) |
| Completion Rate | 62% (by points) |
| Bugs Found | 4 (Keycloak config issues) |
| Bugs Fixed | 4 |
| Additional Deliverables | 5 Sprint 4 stories (Customer Portal) |
| Total Tests | 139+ (10 new in auth-service) |
| PRs Merged | 2 (#20, #31) |

### Velocity Trend

| Sprint | Planned | Completed | Rate | Notes |
|--------|---------|-----------|------|-------|
| Sprint 1 | 16 | 16 | 100% | Foundation + Customer CRUD |
| Sprint 2 | 13 | 13 | 100% | Loan + Document UI |
| Sprint 3 | 13 | 8 | 62% | Auth + RBAC (partial) + Customer Portal (bonus) |

**Note**: While Sprint 3 velocity appears lower, significant additional work was done on the Customer Portal (Sprint 4 stories) which isn't counted in Sprint 3 points.

---

## What Went Well
1. Keycloak OAuth2/OIDC adoption - industry standard, better than custom JWT
2. Role-based navigation working cleanly for Customer vs Staff views
3. Customer Portal delivered ahead of schedule (Sprint 4 stories done early)
4. Bug fixes documented permanently in MEMORY.md and realm-export.json
5. Comprehensive realm export with test users for easy onboarding

## What Could Improve
1. Keycloak configuration issues consumed significant debugging time
2. `sslRequired` and JWT claim format should have been caught during initial setup
3. US-003 admin UI was deferred - no self-service role management yet
4. Sprint scope evolved mid-sprint (Customer Portal added)
5. Need better upfront testing of Keycloak token format before integration

---

## Carryover Items for Future Sprints

| Issue | Title | Remaining Work |
|-------|-------|---------------|
| #4 | [US-003] RBAC Admin UI | Admin panel for user/role management (5 pts) |
| #1 | [EPIC-001] Platform Foundation | Parent epic - close when US-003 done |
| #22 | [EPIC-004] Customer Self-Service Portal | Close - portal delivered via Sprint 4 |
| #21, #23, #24, #25 | Sprint 3 Portal Stories | Reassess - may be covered by Sprint 4 work |

---

## Next Sprint

### Focus: EPIC-003 Policy Engine (from backlog)
- Flowable BPMN workflow engine integration
- Drools DRL decision engine
- Loan processing automation

### Also Consider
- US-003 remaining work (Admin UI for role management)
- Closing Sprint 3 portal issues that are now covered

---

## Sign-Off
- [x] Authentication system fully functional (Keycloak OAuth2/OIDC)
- [x] All critical bugs resolved and documented
- [x] GitHub milestone reviewed
- [x] Customer Portal delivered (bonus deliverable)
- [x] Documentation updated (CLAUDE.md, MEMORY.md, realm-export.json)
