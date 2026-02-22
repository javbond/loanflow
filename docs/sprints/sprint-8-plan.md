# Sprint 8 Plan - CIBIL Integration + Income Verification + Document Upload Enhanced

## Sprint Details

| Field | Value |
|-------|-------|
| Sprint | 8 |
| Duration | 2026-04-03 to 2026-04-17 (2 weeks) |
| Milestone | [Sprint 8](https://github.com/javbond/loanflow/milestone/11) |
| Sprint Goal | Integrate CIBIL credit bureau, add income verification pipeline (ITR/GST/bank statement), and enhance document upload with virus scanning — advancing Milestone 3 (Integration Complete) |
| Velocity Target | 16 story points |

---

## Sprint Backlog

| Issue | Title | Points | Epic | Priority |
|-------|-------|--------|------|----------|
| #44 | [US-016] Credit Bureau Integration (CIBIL API) | 8 | EPIC-005 | P1 |
| #45 | [US-017] Income Verification (ITR, GST, Bank Statement) | 5 | EPIC-005 | P1 |
| #46 | [US-020] Document Upload Enhanced (Drag-Drop, Validation, Virus Scan) | 3 | EPIC-006 | P2 |

**Total**: 16 story points

---

## User Stories Detail

### US-016: Credit Bureau Integration — CIBIL API (8 points) - #44

**As a** loan officer, **I want to** pull a real-time CIBIL credit report for applicants, **so that** I can assess creditworthiness using actual bureau data instead of simulated scores.

#### Strategic Context
Sprint 7 built Drools with simulated credit data (random CIBIL scores). Sprint 8 replaces simulation with real CIBIL API integration. The existing `CreditCheckDelegate` orchestrates Drools — we add a CIBIL client upstream that feeds real data into `CreditReportFact`.

#### Architecture Decision
- **CIBIL API**: Mock/stub in dev environment (realistic response payloads), with configurable toggle for production API
- **Caching**: Redis with 24h TTL (CIBIL charges per pull, avoid duplicate charges)
- **Fallback**: If bureau unavailable → use cached response or fall back to simulated data with a `bureauDataSource: SIMULATED` flag

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | CIBIL API client | REST template + retry logic (3 retries, exponential backoff) | 4h |
| T2 | Credit report parser | Parse CIBIL response: score, accounts, enquiries, DPD, write-offs | 3h |
| T3 | Drools integration | Feed parsed data into `CreditReportFact` replacing simulated data | 2h |
| T4 | Redis caching | Cache bureau responses with 24h TTL, keyed by PAN | 2h |
| T5 | Fallback mechanism | Timeout/error handling → cached response or simulated fallback | 2h |
| T6 | Credit report component | Angular component showing full credit report (score, accounts, history) | 3h |
| T7 | REST API endpoint | `POST /api/v1/credit-bureau/pull` for manual credit pulls | 1h |
| T8 | TDD unit tests | 15+ tests covering client, parser, cache, fallback, integration | 3h |

#### Dependencies
- `CreditCheckDelegate.java` (Sprint 7) ✅ — orchestrates credit check in BPMN
- `CreditReportFact.java` (Sprint 7) ✅ — Drools fact model for credit data
- Redis infrastructure ✅ — already used by policy-service

---

### US-017: Income Verification — ITR, GST, Bank Statement (5 points) - #45

**As an** underwriter, **I want to** verify applicant income through ITR returns, GST filings, and bank statement analysis, **so that** I can assess repayment capacity accurately.

#### Architecture Decision
- **ITR & GST APIs**: Mock/stub services with realistic data — configurable toggle for production APIs
- **Bank statement**: Structured data analysis (CSV/JSON) — not PDF OCR (that's US-022 scope)
- **DTI Calculator**: `totalMonthlyObligations / grossMonthlyIncome` — feeds into Drools for enhanced scoring

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | ITR verification client | Fetch last 2 years' ITR data (mock/stub) | 3h |
| T2 | GST return fetcher | Fetch last 12 months' GST returns by GSTIN (mock/stub) | 2h |
| T3 | Bank statement analyzer | Parse structured data: avg balance, regular credits, income pattern | 3h |
| T4 | DTI/EMI calculator | Debt-to-income and EMI/NMI ratio computation | 2h |
| T5 | Income verification UI | Angular component showing income summary, DTI, EMI/NMI | 2h |
| T6 | TDD unit tests | 10+ tests covering all services and calculators | 2h |

#### Dependencies
- customer-service (applicant data) ✅
- US-016 (parallel development, no blocking dependency)

---

### US-020: Document Upload Enhanced (3 points) - #46

**As a** customer, **I want to** upload documents with drag-and-drop, file validation, and virus scanning, **so that** the upload process is smooth and secure.

#### Architecture Decision
- **ClamAV**: Docker container added to `docker-compose.yml`, Spring Boot integration via `clamav-client` library
- **Drag-drop**: Angular CDK DragDrop for the upload zone
- **Validation**: Client-side (PDF/JPG/PNG, max 10MB) + server-side double-check

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | Drag-drop upload | Replace basic file input with drag-drop zone component | 2h |
| T2 | File validation | Client-side type (PDF/JPG/PNG) and size (max 10MB) validation | 1h |
| T3 | ClamAV integration | Docker setup + Spring Boot virus scan before MinIO storage | 3h |
| T4 | Progress indicator | Upload progress bar using HttpClient reportProgress | 1h |
| T5 | TDD unit tests | 5+ tests for validation, scan, upload pipeline | 1h |

#### Dependencies
- document-service ✅ — existing MinIO storage pipeline
- Docker Compose ✅ — add ClamAV container

---

## Execution Plan

### Week 1 (2026-04-03 to 2026-04-10)

| Day | Focus | Stories |
|-----|-------|---------|
| Mon | Sprint kick-off, CIBIL client + parser | US-016 T1-T2 |
| Tue | CIBIL Drools integration + caching | US-016 T3-T4 |
| Wed | CIBIL fallback + REST API | US-016 T5, T7 |
| Thu | CIBIL Angular component + tests | US-016 T6, T8 |
| Fri | Document upload (drag-drop, validation, ClamAV) | US-020 T1-T5 |

### Week 2 (2026-04-10 to 2026-04-17)

| Day | Focus | Stories |
|-----|-------|---------|
| Mon | ITR + GST verification clients | US-017 T1-T2 |
| Tue | Bank statement analyzer | US-017 T3 |
| Wed | DTI/EMI calculator + Angular component | US-017 T4-T5 |
| Thu | Income verification tests + integration | US-017 T6 |
| Fri | Sprint review + UAT | All stories |

---

## Definition of Done

- [ ] Code complete with unit tests (>80% coverage)
- [ ] All 3 stories implemented and unit-tested
- [ ] CIBIL mock API returns realistic data in dev/UAT
- [ ] Redis caching verified (hit/miss scenarios)
- [ ] ClamAV Docker container operational
- [ ] Angular components render correctly for all stories
- [ ] Full E2E workflow: submit loan → CIBIL pull → income verification → underwriting
- [ ] GitHub issues closed with completion comments
- [ ] Sprint plan and CLAUDE.md updated

---

## Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| CIBIL API format changes | High | Low | Use versioned mock; parser handles multiple formats |
| ClamAV Docker setup complexity | Medium | Medium | Conditional config — skip scan if ClamAV unavailable |
| ITR/GST API availability | Medium | Low | Mock/stub with toggle; production API later |
| Redis connection issues | Medium | Low | Existing infra proven in Sprint 5-6; fallback to no-cache |

---

## Infrastructure Changes

| Component | Change |
|-----------|--------|
| `docker-compose.yml` | Add ClamAV container |
| Redis | Add CIBIL cache namespace (existing instance) |
| loan-service `pom.xml` | Add `cibil-client` (mock), Redis cache config |
| document-service `pom.xml` | Add `clamav-client` dependency |
