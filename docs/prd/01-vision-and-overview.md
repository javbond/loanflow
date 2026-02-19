# LoanFlow - Vision and Overview

## Executive Summary

**Product Name:** LoanFlow - Enterprise Loan Origination System
**Target Market:** Indian Banks (Scheduled Commercial Banks, Small Finance Banks, NBFCs)
**Timeline:** 3-4 months for MVP (Aggressive)
**Approach:** Domain-Driven Design (DDD) + Agile/Scrum + TDD

---

## 1.1 Vision Statement

Build an enterprise-grade, regulatory-compliant Loan Origination System that enables Indian banks to:
- Process retail, corporate, and priority sector loans through a unified platform
- Define dynamic lending policies without code changes (Entra-style policy engine)
- Ensure 100% RBI compliance with automated checks
- Reduce loan processing time by 60% through intelligent automation

## 1.2 Problem Statement

Indian banks face challenges with:
1. **Legacy Systems**: Monolithic, inflexible loan processing systems
2. **Compliance Burden**: Manual RBI reporting, KYC verification, CERSAI registration
3. **Slow Turnaround**: 7-15 days for retail loans, 30+ days for corporate
4. **Policy Rigidity**: Code changes required for new products or policy updates
5. **Integration Gaps**: Disconnected systems for credit bureau, e-KYC, GST verification

## 1.3 Target Users

| User Persona | Role | Primary Goals |
|--------------|------|---------------|
| **Loan Officer** | Branch staff | Quick application capture, document collection |
| **Credit Analyst** | Underwriting | Risk assessment, credit memo preparation |
| **Underwriter** | Decision maker | Approve/reject with conditions, set terms |
| **Branch Manager** | Approver | High-value approvals, exception handling |
| **Compliance Officer** | Regulatory | RBI reporting, audit trails, KYC compliance |
| **Product Manager** | Bank staff | Define new loan products, modify policies |
| **IT Admin** | Technical | System configuration, user management |
| **Customer** | Applicant | Self-service application, document upload, status tracking |

## 1.4 Loan Products Scope

### MVP (Month 1-3)
| Product | Type | Priority |
|---------|------|----------|
| Personal Loan | Retail | P0 |
| Home Loan | Retail | P0 |
| Vehicle Loan | Retail | P1 |
| Gold Loan | Retail/Priority | P1 |
| MSME Working Capital | Corporate | P1 |
| Kisan Credit Card (KCC) | Priority Sector | P1 |

### Phase 2 (Month 4-6)
- Education Loan
- Loan Against Property (LAP)
- Term Loan (Corporate)
- Trade Finance (LC, BG)
- Agricultural Term Loan

### Phase 3 (Month 7-12)
- Consortium/Syndicated Loans
- Project Finance
- Restructured Loans
- NPA Management

## 1.5 Success Metrics (KPIs)

| Metric | Target | Measurement |
|--------|--------|-------------|
| Loan TAT (Personal Loan) | < 4 hours | End-to-end processing time |
| Loan TAT (Home Loan) | < 3 days | Excluding external dependencies |
| Auto-Approval Rate | > 40% | Straight-through processing |
| Policy Change Deployment | < 1 hour | No-code policy updates |
| System Uptime | 99.9% | Excluding planned maintenance |
| Compliance Score | 100% | RBI audit findings |

## 1.6 Technology Stack

| Layer | Technology | Version | Justification |
|-------|-----------|---------|---------------|
| **Frontend** | Angular | 17+ | Enterprise support, reactive forms, strong typing |
| **UI Library** | PrimeNG | 17+ | Rich components, Indian locale support |
| **State Mgmt** | NgRx | 17+ | Predictable state, debugging tools |
| **API Gateway** | Kong / AWS API Gateway | Latest | Rate limiting, auth, WAF integration |
| **Backend** | Spring Boot | 3.2+ | Mature ecosystem, enterprise features |
| **Security** | Spring Security + Keycloak | Latest | OAuth2, OIDC, SSO support |
| **Workflow** | Flowable | 7.2.0 | BPMN 2.0, Apache 2.0 license, free |
| **Rules Engine** | Drools | 9.x | Apache 2.0, powerful rule chaining |
| **Primary DB** | PostgreSQL | 16 | ACID, JSON support, enterprise ready |
| **Document DB** | MongoDB | 7.x | Flexible schema for policies |
| **Cache** | Redis | 7.x | Session, policy cache, rate limiting |
| **Search** | Elasticsearch | 8.x | Audit logs, full-text search |
| **Message Queue** | Apache Kafka | 3.x | Event sourcing, async processing |
| **Object Storage** | MinIO | Latest | S3-compatible, on-premise ready |
| **Containerization** | Docker + Kubernetes | Latest | Scalability, cloud-agnostic |
