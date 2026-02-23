# Sprint 11 Plan - Hardening & Production Readiness (EPIC-008)

## Sprint Details

| Field | Value |
|-------|-------|
| Sprint | 11 |
| Duration | 2026-02-23 to 2026-03-09 (2 weeks) |
| Milestone | [Sprint 11](https://github.com/javbond/loanflow/milestone/14) |
| Sprint Goal | Harden the platform for production readiness — integration tests with Testcontainers, API Gateway (Spring Cloud Gateway), Docker Compose full-stack deployment, documentation debt closure |
| Velocity Target | 16 story points |
| Parent Milestone | M4: Production Ready |

---

## Sprint Backlog

| Issue | Title | Points | Epic | Priority |
|-------|-------|--------|------|----------|
| #57 | [US-032] Integration Tests with Testcontainers | 8 | EPIC-008 | P1 |
| #58 | [US-033] API Gateway (Spring Cloud Gateway) | 5 | EPIC-008 | P1 |
| #59 | [US-034] Docker Compose Full-Stack + Documentation Debt | 3 | EPIC-008 | P1 |

**Total**: 16 story points

---

## User Stories Detail

### US-032: Integration Tests with Testcontainers (8 points) - #57

**As a** developer, **I want to** have integration tests that run against real database instances via Testcontainers, **so that** I have confidence the system works end-to-end beyond mocked unit tests.

#### Current State
- 525 unit tests with Mockito — zero integration tests
- Testcontainers 1.19.3 BOM already declared in root pom.xml but unused
- `de.flapdoodle.embed.mongo` available for document-service

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | loan-service integration tests | `@SpringBootTest` + Testcontainers PostgreSQL — test LoanApplicationService create/submit/approve flows with real DB, Flowable embedded engine | 4h |
| T2 | customer-service integration tests | `@SpringBootTest` + Testcontainers PostgreSQL — test CustomerService CRUD + e-KYC status tracking with real DB | 3h |
| T3 | document-service integration tests | `@SpringBootTest` + Flapdoodle embedded MongoDB — test DocumentService CRUD + audit event storage | 3h |
| T4 | policy-service integration tests | `@SpringBootTest` + Flapdoodle embedded MongoDB + embedded Redis — test PolicyService CRUD + evaluation | 3h |
| T5 | notification-service integration tests | `@SpringBootTest` + Testcontainers RabbitMQ — test consumer → dispatcher → email channel with real message broker | 2h |
| T6 | Shared test configuration | `application-integration-test.yml` per service, Maven profile `integration-tests` | 2h |
| T7 | Base test classes and utilities | Abstract base test class, test data builders | 1h |

#### Acceptance Criteria
- [ ] Given a clean database, when `mvn verify -Pintegration-tests` runs, then all integration tests pass
- [ ] Given Testcontainers is available, when loan-service IT runs, then it creates real PostgreSQL + Flowable process instances
- [ ] At least 5 integration test classes across 4+ services

---

### US-033: API Gateway — Spring Cloud Gateway (5 points) - #58

**As a** platform operator, **I want** a single API entry point that routes to all microservices, **so that** the frontend only needs one base URL and we can add cross-cutting concerns centrally.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | gateway-service module | New `api-gateway` module in backend, Spring Cloud Gateway reactive, port 8080 | 2h |
| T2 | Route configuration | Routes to customer-service (8082), loan-service (8081), document-service (8083), auth-service (8085), policy-service (8086), notification-service (8084) | 2h |
| T3 | JWT token relay | Forward Keycloak JWT to downstream services, centralized CORS config | 2h |
| T4 | Angular proxy update | Update `proxy.conf.json` and environment.ts to point to gateway (8080) | 1h |
| T5 | Health check aggregation | Actuator health endpoint aggregating downstream service health | 1h |
| T6 | Gateway tests | Route resolution tests, JWT relay tests, fallback tests | 2h |

#### Acceptance Criteria
- [ ] Given gateway is running on 8080, when frontend calls `/api/v1/loans`, then it routes to loan-service:8081
- [ ] Given a valid JWT, when request passes through gateway, then downstream service receives the JWT
- [ ] Given all services are healthy, when `/actuator/health` is called, then aggregated status is UP

---

### US-034: Docker Compose Full-Stack + Documentation Debt (3 points) - #59

**As a** developer, **I want** a single docker-compose command that starts the entire platform, and complete sprint documentation, **so that** new team members can onboard quickly and project history is traceable.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | docker-compose.services.yml | New compose file with all 7 backend services + frontend, using existing Dockerfiles | 2h |
| T2 | Build script | Shell script to build all service JARs and Docker images in correct order | 1h |
| T3 | Sprint 8 review doc | Create `docs/sprints/sprint-8-review.md` following established template | 1h |
| T4 | Missing sprint plan docs | Create `sprint-1-plan.md`, `sprint-4-plan.md`, `sprint-9-plan.md` (retroactive) | 2h |
| T5 | Gateway in Docker Compose | Add api-gateway to compose, wire up networking | 1h |
| T6 | README.md update | Update root README with full-stack startup instructions | 1h |

#### Acceptance Criteria
- [ ] Docker Compose starts all services with a single command
- [ ] Sprint documentation complete for Sprints 1–10
- [ ] Root README has setup and startup instructions

---

## Technical Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Testcontainers | 1.19.3 | Real DB integration tests |
| Spring Cloud Gateway | 2023.0.0 | API Gateway (reactive) |
| spring-cloud-starter-gateway | 4.1.x | Gateway autoconfiguration |
| Flapdoodle embedded MongoDB | 4.9.2 | MongoDB integration tests |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Testcontainers requires Docker daemon | Medium | CI has Docker; devs already use Docker for infra |
| Spring Cloud Gateway version compatibility | Low | Use Spring Cloud BOM matching Spring Boot 3.2.2 |
| Integration tests slow CI pipeline | Medium | Separate Maven profile, run only on PR merge |
