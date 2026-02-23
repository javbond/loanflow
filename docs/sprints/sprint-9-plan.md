# Sprint 9 Plan - Document Lifecycle (EPIC-006 Completion)

## Sprint Details

| Field | Value |
|-------|-------|
| Sprint | 9 |
| Duration | 2026-02-23 |
| Milestone | [Sprint 9](https://github.com/javbond/loanflow/milestone/12) |
| Sprint Goal | Complete EPIC-006 (Document Management System) with verification workflow, OCR data extraction, and sanction letter generation |
| Velocity Target | 16 story points |
| Parent Milestone | M3: Integration Complete |

---

## Sprint Backlog

| Issue | Title | Points | Epic | Priority |
|-------|-------|--------|------|----------|
| #47 | [US-021] Document Verification Workflow | 5 | EPIC-006 | P1 |
| #48 | [US-022] OCR & Data Extraction | 8 | EPIC-006 | P1 |
| #49 | [US-023] Sanction Letter Generation | 3 | EPIC-006 | P1 |

**Total**: 16 story points

---

## User Stories Detail

### US-021: Document Verification Workflow (5 points) - #47

**As a** loan officer, **I want** a document verification workflow that checks completeness and allows batch verification, **so that** I can efficiently verify all required documents before underwriting.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | Document requirements config | Per-loan-type required documents via @ConfigurationProperties | 2h |
| T2 | Completeness checklist API | Required/uploaded/verified tracking per application | 2h |
| T3 | Batch verification endpoint | Verify/reject multiple documents in one API call | 2h |
| T4 | DocumentPanelComponent | Angular UI with progress bar, document cards, verify/reject buttons | 3h |
| T5 | Unit tests | Service and controller tests | 2h |

### US-022: OCR & Data Extraction (8 points) - #48

**As a** system, **I want** to automatically extract data from uploaded documents using OCR, **so that** manual data entry is reduced.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | Apache Tika integration | Text extraction from PDF/images | 3h |
| T2 | Document type extractors | Aadhaar, PAN, salary slip, bank statement parsers | 4h |
| T3 | Extraction API and domain model | ExtractionResult entity, extraction service | 2h |
| T4 | Confidence scoring | Regex-based extraction confidence assessment | 2h |
| T5 | Angular extraction results display | Extracted fields panel in document detail | 2h |
| T6 | Unit tests | Extractor and service tests | 3h |

### US-023: Sanction Letter Generation (3 points) - #49

**As a** loan officer, **I want** to generate PDF sanction letters for approved loans, **so that** borrowers receive official approval documentation.

#### Tasks

| # | Task | Description | Est. |
|---|------|-------------|------|
| T1 | PDF generation engine | iText/OpenPDF for sanction letter creation | 2h |
| T2 | Template design | HTML/Thymeleaf template for sanction letter layout | 2h |
| T3 | Generation API | REST endpoint to generate and store sanction letter | 1h |
| T4 | Auto-upload to MinIO | Store generated PDF as a document in document-service | 1h |
| T5 | Unit tests | Generation and template tests | 1h |

---

## Definition of Done

- [x] All 3 stories complete with unit tests
- [x] Document verification workflow working end-to-end
- [x] OCR extraction working for Aadhaar, PAN, salary slip, bank statement
- [x] Sanction letter PDF generation and storage working
- [x] PR merged to main
