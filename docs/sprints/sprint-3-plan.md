# Sprint 3 Plan - Security & Authentication

**Sprint Duration**: 2026-02-20 to 2026-03-06 (2 weeks)
**Sprint Goal**: Implement complete authentication and authorization system
**Total Story Points**: 13
**Velocity (Previous)**: 13 points

---

## Sprint Backlog

### EPIC-001: Platform Foundation (#1)

#### US-002: Authentication System (8 points) - #3

**User Story**: As a user, I want to securely login and logout of the system so that my data is protected.

| Task | Description | Estimate | Status |
|------|-------------|----------|--------|
| T1 | Setup auth-service Spring Boot module | 2h | ⏳ |
| T2 | Implement User entity with BCrypt password | 2h | ⏳ |
| T3 | Create JWT token service (issue/validate) | 3h | ⏳ |
| T4 | Implement login/register endpoints | 3h | ⏳ |
| T5 | Create Angular auth service & interceptor | 3h | ⏳ |
| T6 | Build Login component (Material Design) | 2h | ⏳ |
| T7 | Implement AuthGuard for protected routes | 2h | ⏳ |
| T8 | Add user session management (localStorage) | 2h | ⏳ |
| T9 | Write unit tests (>80% coverage) | 3h | ⏳ |

**Acceptance Criteria**:
- [ ] Valid credentials → JWT token issued
- [ ] Invalid credentials → error message
- [ ] Valid token → API request authorized
- [ ] Expired token → 401 returned
- [ ] Logout → session cleared

---

#### US-003: Role-Based Access Control (5 points) - #4

**User Story**: As an admin, I want to assign roles to users so that they can only access appropriate features.

| Task | Description | Estimate | Status |
|------|-------------|----------|--------|
| T1 | Define Role enum (ADMIN, LOAN_OFFICER, UNDERWRITER, CUSTOMER) | 1h | ⏳ |
| T2 | Create UserRole entity & repository | 2h | ⏳ |
| T3 | Implement @PreAuthorize on all controllers | 2h | ⏳ |
| T4 | Create role assignment API endpoints | 2h | ⏳ |
| T5 | Build admin UI for user/role management | 3h | ⏳ |
| T6 | Update services to use real user ID from JWT | 2h | ⏳ |
| T7 | Write integration tests | 2h | ⏳ |

**Acceptance Criteria**:
- [ ] ADMIN can assign/remove roles
- [ ] LOAN_OFFICER can access loan features
- [ ] UNDERWRITER can access verification
- [ ] CUSTOMER cannot access admin features
- [ ] Unauthorized API calls return 403

---

## Technical Architecture

### Backend (auth-service)
```
backend/auth-service/
├── src/main/java/com/loanflow/auth/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── JwtConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── UserController.java
│   ├── domain/
│   │   ├── entity/User.java
│   │   ├── entity/Role.java
│   │   └── enums/RoleType.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── RoleRepository.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   └── UserService.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── AuthResponse.java
└── src/main/resources/
    └── application.yml
```

### Frontend (Angular)
```
frontend/loanflow-web/src/app/
├── core/
│   ├── auth/
│   │   ├── services/
│   │   │   └── auth.service.ts
│   │   ├── guards/
│   │   │   └── auth.guard.ts
│   │   ├── interceptors/
│   │   │   └── auth.interceptor.ts
│   │   └── models/
│   │       └── auth.model.ts
│   └── components/
│       └── login/
│           ├── login.component.ts
│           ├── login.component.html
│           └── login.component.scss
└── features/
    └── admin/
        └── components/
            └── user-management/
```

---

## Roles Definition

| Role | Description | Access |
|------|-------------|--------|
| ADMIN | System administrator | Full access to all features |
| LOAN_OFFICER | Creates/manages loans | Customers, Loans, Documents |
| UNDERWRITER | Reviews/approves loans | Loans (review), Documents (verify) |
| SENIOR_UNDERWRITER | Senior approval authority | Same as Underwriter + higher limits |
| CUSTOMER | End user/applicant | Own profile, Own applications |

---

## API Endpoints

### Auth Controller
| Method | Endpoint | Description | Public |
|--------|----------|-------------|--------|
| POST | /api/v1/auth/register | Register new user | Yes |
| POST | /api/v1/auth/login | Login and get JWT | Yes |
| POST | /api/v1/auth/refresh | Refresh JWT token | No |
| POST | /api/v1/auth/logout | Invalidate session | No |
| GET | /api/v1/auth/me | Get current user | No |

### User Controller (Admin)
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | /api/v1/users | List all users | ADMIN |
| GET | /api/v1/users/{id} | Get user by ID | ADMIN |
| PUT | /api/v1/users/{id}/roles | Assign roles | ADMIN |
| DELETE | /api/v1/users/{id}/roles/{role} | Remove role | ADMIN |

---

## Dependencies

- Spring Security 6.x
- JJWT (Java JWT library)
- BCrypt password encoder
- Angular JWT helper

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| JWT secret exposure | Use environment variables |
| Token theft | Short expiry + refresh tokens |
| Brute force attacks | Rate limiting on login |
| CORS issues | Proper CORS configuration |

---

## Success Metrics

- [ ] All 13 story points delivered
- [ ] >80% test coverage on auth-service
- [ ] UAT mode can be deprecated
- [ ] Real user IDs used instead of placeholders
