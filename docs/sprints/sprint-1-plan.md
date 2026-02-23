# Sprint 1 Plan - Platform Foundation & Customer Management

## Sprint Details

| Field | Value |
|-------|-------|
| Sprint | 1 |
| Duration | 2026-02-15 to 2026-02-19 |
| Milestone | [M1: Core Platform](https://github.com/javbond/loanflow/milestone/1) |
| Sprint Goal | Establish project foundation with infrastructure, backend microservices, and complete Customer Management module end-to-end |
| Velocity Target | 16 story points (first sprint baseline) |

---

## Sprint Backlog

| Issue | Title | Points | Epic | Priority |
|-------|-------|--------|------|----------|
| #2 | [US-001] Project Setup & Scaffolding | 8 | EPIC-001 | P0 |
| #13 | [US-004] Customer Management Module | 8 | EPIC-001 | P0 |

**Total**: 16 story points

---

## User Stories Detail

### US-001: Project Setup & Scaffolding (8 points) - #2

**As a** developer, **I want** the complete project infrastructure scaffolded, **so that** all services have a consistent foundation to build upon.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | Parent POM and multi-module Maven structure | Spring Boot 3.2.2, Java 20, common modules | 3h |
| T2 | Docker Compose infrastructure | PostgreSQL, MongoDB, MinIO, Redis, RabbitMQ, Keycloak | 3h |
| T3 | Common modules | common-dto, common-security, common-utils | 2h |
| T4 | Auth service scaffold | Keycloak integration, JWT validation | 2h |
| T5 | Flyway migrations setup | Schema creation for customer_db, loan_db | 1h |
| T6 | Angular 17 frontend scaffold | Project creation, Angular Material, routing | 2h |
| T7 | Database init scripts | PostgreSQL multi-database initialization | 1h |
| T8 | CI/CD base configuration | Git repo, gitignore, initial README | 1h |

### US-004: Customer Management Module (8 points) - #13

**As a** bank staff member, **I want** to manage customer records via CRUD APIs and UI, **so that** loan applications can be linked to verified customers.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | Customer entity with JPA | Customer model, status lifecycle, Flyway migration | 2h |
| T2 | Customer repository | Spring Data JPA repository with custom queries | 1h |
| T3 | Customer service layer | CRUD operations, validation, business logic | 3h |
| T4 | Customer REST controller | Full CRUD API with pagination and search | 2h |
| T5 | Customer Angular components | List, form, detail views | 4h |
| T6 | Unit tests | Service and controller tests | 3h |

---

## Definition of Done

- [x] Code complete with unit tests
- [x] Docker infrastructure starts cleanly
- [x] Customer CRUD API functional via Swagger
- [x] Angular frontend serves with customer management
