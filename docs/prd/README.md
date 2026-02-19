# LoanFlow PRD Documentation

This directory contains the Product Requirements Document (PRD) and implementation plan for the LoanFlow Enterprise Loan Origination System.

## Document Index

| File | Description |
|------|-------------|
| [01-vision-and-overview.md](./01-vision-and-overview.md) | Vision statement, problem statement, target users, loan products scope, success metrics, and technology stack |
| [02-functional-requirements.md](./02-functional-requirements.md) | All functional requirements (FR-001 through FR-007) covering loan application, policy engine, workflow, credit assessment, documents, compliance, and disbursement |
| [03-non-functional-requirements.md](./03-non-functional-requirements.md) | All non-functional requirements (NFR-001 through NFR-005) covering performance, security, availability, scalability, and compliance |
| [04-hld-architecture.md](./04-hld-architecture.md) | High-level design including system architecture, bounded contexts (DDD), context maps, database schema, and API specifications |
| [05-lld-tactical-design.md](./05-lld-tactical-design.md) | Low-level design with DDD tactical patterns: aggregates, entities, value objects, domain events, and testing strategy |
| [06-backlog-epics.md](./06-backlog-epics.md) | Complete backlog with 7 epics, 27 user stories, and associated tasks organized by sprint |
| [07-milestones-roadmap.md](./07-milestones-roadmap.md) | Product roadmap, 4 milestones, sprint timeline, CI/CD pipeline, and risk register |

## Quick Reference

### Project Overview

- **Product:** LoanFlow - Enterprise Loan Origination System
- **Target Market:** Indian Banks (SCBs, SFBs, NBFCs)
- **Timeline:** 3-4 months for MVP
- **Methodology:** DDD + Agile/Scrum + TDD

### Key Technologies

| Layer | Technology |
|-------|------------|
| Frontend | Angular 17+, PrimeNG, NgRx |
| Backend | Spring Boot 3.2+, Spring Security |
| Auth | Keycloak (OAuth2/OIDC) |
| Workflow | Flowable (BPMN 2.0) |
| Rules | Drools 9.x |
| Databases | PostgreSQL 16, MongoDB 7.x |
| Cache | Redis 7.x |
| Search | Elasticsearch 8.x |
| Messaging | Apache Kafka 3.x |
| Storage | MinIO (S3-compatible) |

### Milestones

1. **M1: Core Platform Ready** - End of Month 1
2. **M2: Policy Engine Live** - End of Month 2
3. **M3: Integration Complete** - End of Month 3
4. **M4: Production Ready** - End of Month 4

### Epics Summary

| Epic | Name | Sprint |
|------|------|--------|
| EPIC-001 | Platform Foundation | 1 |
| EPIC-002 | Loan Application Management | 2 |
| EPIC-003 | Dynamic Policy Engine | 3-4 |
| EPIC-004 | Workflow Engine | 3-4 |
| EPIC-005 | Credit & Risk Assessment | 4-5 |
| EPIC-006 | Document Management | 4-5 |
| EPIC-007 | Regulatory Compliance | 5-6 |
