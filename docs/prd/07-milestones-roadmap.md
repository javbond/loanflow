# LoanFlow - Milestones and Roadmap

## Product Roadmap

```
                           PRODUCT ROADMAP

  MONTH 1                 MONTH 2                 MONTH 3                MONTH 4
  -------                 -------                 -------                -------

  +-------------+        +-------------+        +-------------+        +-------+
  | Foundation  |        | Core Engine |        | Integrations|        |  UAT  |
  |             |        |             |        |             |        |  & Go |
  | Sprint 1-2  |------->| Sprint 3-4  |------->| Sprint 5-6  |------->| Live  |
  +-------------+        +-------------+        +-------------+        +-------+
        |                      |                      |                     |
        v                      v                      v                     v
  - Project Setup        - Policy Engine        - CIBIL Integration   - UAT
  - DDD Domain Model     - Workflow Engine      - e-KYC (Aadhaar)     - Bug Fix
  - Core Entities        - Decision Engine      - CERSAI              - Go-Live
  - Auth/Identity        - Document Mgmt        - GST Verification    - Training
  - Basic CRUD           - Underwriting UI      - Notification
  - DB Schema            - Applicant Portal     - Reporting
```

---

## Milestone Details

### Milestone 1: Core Platform Ready

**Target:** End of Month 1

**Deliverables:**
- Loan application CRUD
- User authentication
- Basic workflow

**Acceptance Criteria:**
- [ ] Users can create and submit loan applications
- [ ] Authentication via Keycloak is functional
- [ ] Basic workflow moves applications through stages
- [ ] PostgreSQL and MongoDB schemas are deployed
- [ ] CI/CD pipeline is operational

---

### Milestone 2: Policy Engine Live

**Target:** End of Month 2

**Deliverables:**
- Dynamic policy creation
- Rule-based decisioning
- Workflow orchestration

**Acceptance Criteria:**
- [ ] Admin can create policies via UI without code
- [ ] Policies support AND/OR conditions
- [ ] Policy versioning and rollback works
- [ ] Drools decision engine evaluates rules
- [ ] Flowable workflow routes applications correctly
- [ ] Document upload and verification functional

---

### Milestone 3: Integration Complete

**Target:** End of Month 3

**Deliverables:**
- Credit bureau connected
- e-KYC functional
- Regulatory compliance

**Acceptance Criteria:**
- [ ] CIBIL integration fetches credit scores
- [ ] UIDAI e-KYC verifies applicants
- [ ] CERSAI registration for secured loans
- [ ] GST verification for business loans
- [ ] Email/SMS notifications working
- [ ] RBI compliance reports generated

---

### Milestone 4: Production Ready

**Target:** End of Month 4

**Deliverables:**
- UAT signed off
- Security audit passed
- Go-live approved

**Acceptance Criteria:**
- [ ] All UAT test cases passed
- [ ] OWASP security scan passed
- [ ] Performance benchmarks met
- [ ] Documentation complete
- [ ] Training materials delivered
- [ ] Go-live checklist completed

---

## Sprint-wise Timeline

| Sprint | Duration | Focus | Milestone |
|--------|----------|-------|-----------|
| Sprint 1 | Week 1-2 | Foundation, Auth, Project Setup | M1: Core Platform |
| Sprint 2 | Week 3-4 | Loan Application CRUD, Basic Workflow | M1: Core Platform |
| Sprint 3 | Week 5-6 | Policy Engine, Condition Builder | M2: Policy Engine |
| Sprint 4 | Week 7-8 | Decision Engine, Underwriting | M2: Policy Engine |
| Sprint 5 | Week 9-10 | CIBIL, e-KYC, Document Mgmt | M3: Integrations |
| Sprint 6 | Week 11-12 | Testing, Bug Fixes, UAT | M4: Production Ready |

---

## CI/CD Pipeline

```yaml
# .github/workflows/ci.yml

name: CI Pipeline

on:
  push:
    branches: [main, develop, 'feature/**']
  pull_request:
    branches: [main, develop]

jobs:
  # Stage 1: Build & Unit Tests
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Build Backend
        run: mvn clean verify -DskipITs
      - name: Build Frontend
        run: |
          cd frontend/loanflow-ui
          npm ci && npm run build

  # Stage 2: Code Quality
  sonarqube:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: SonarQube Scan
        run: mvn sonar:sonar

  # Stage 3: Security Scan
  security:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Snyk Vulnerability Scan
        uses: snyk/actions/maven@master
      - name: OWASP Dependency Check
        run: mvn dependency-check:check
      - name: Secret Scanning
        uses: trufflesecurity/trufflehog@main

  # Stage 4: Integration Tests
  integration:
    needs: [sonarqube, security]
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
      mongodb:
        image: mongo:7
      redis:
        image: redis:7
    steps:
      - name: Run Integration Tests
        run: mvn verify -DskipUTs

  # Stage 5: Deploy to Staging (on main branch)
  deploy-staging:
    needs: integration
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Kubernetes (Staging)
        run: kubectl apply -k infrastructure/kubernetes/overlays/staging
```

---

## Phase 2 Roadmap (Month 4-6)

| Feature | Description | Target |
|---------|-------------|--------|
| Education Loan | New loan product with specific rules | Sprint 7 |
| Loan Against Property (LAP) | Secured loan with property valuation | Sprint 7-8 |
| Term Loan (Corporate) | Corporate lending module | Sprint 8-9 |
| Trade Finance (LC, BG) | Letter of Credit, Bank Guarantee | Sprint 9-10 |
| Agricultural Term Loan | Priority sector lending | Sprint 10 |
| Advanced Analytics | ML-based risk scoring | Sprint 11-12 |

---

## Phase 3 Roadmap (Month 7-12)

| Feature | Description | Target |
|---------|-------------|--------|
| Consortium/Syndicated Loans | Multi-bank lending | Sprint 13-15 |
| Project Finance | Large infrastructure financing | Sprint 16-18 |
| Restructured Loans | Loan modification workflows | Sprint 19-20 |
| NPA Management | Non-performing asset tracking | Sprint 21-22 |
| Mobile App | Native iOS/Android apps | Sprint 23-26 |

---

## Risk Register

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Credit bureau API delays | High | Medium | Implement mock services, parallel development |
| Keycloak integration issues | High | Low | Early POC in Sprint 1 |
| Flowable learning curve | Medium | Medium | Training, documentation review |
| RBI compliance gaps | Critical | Low | Early compliance review, legal consultation |
| Performance bottlenecks | High | Medium | Load testing from Sprint 4 |
| Team attrition | High | Low | Knowledge sharing, documentation |

---

## Success Criteria Summary

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Loan TAT (Personal Loan) | < 4 hours | End-to-end processing time |
| Loan TAT (Home Loan) | < 3 days | Excluding external dependencies |
| Auto-Approval Rate | > 40% | Straight-through processing |
| Policy Change Deployment | < 1 hour | No-code policy updates |
| System Uptime | 99.9% | Excluding planned maintenance |
| Compliance Score | 100% | RBI audit findings |
| Test Coverage (Unit) | > 80% | SonarQube metrics |
| Security Vulnerabilities | 0 Critical/High | Snyk/OWASP scans |
