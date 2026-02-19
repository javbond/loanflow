# LoanFlow - Functional Requirements

## FR-001: Loan Application Management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-001.1 | System shall capture loan applications via web portal, mobile, and branch | P0 |
| FR-001.2 | System shall support multi-applicant (primary + co-applicants) | P0 |
| FR-001.3 | System shall auto-save application drafts | P1 |
| FR-001.4 | System shall validate PAN, Aadhaar format before submission | P0 |
| FR-001.5 | System shall generate unique Application Reference Number (ARN) | P0 |
| FR-001.6 | System shall support application amendment before final submission | P1 |

---

## FR-002: Policy Engine

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-002.1 | Admin shall define eligibility policies via UI (no code) | P0 |
| FR-002.2 | Policies shall support AND/OR condition grouping | P0 |
| FR-002.3 | Policies shall be version-controlled with rollback | P0 |
| FR-002.4 | Policies shall support effective date ranges | P1 |
| FR-002.5 | System shall evaluate all applicable policies in priority order | P0 |
| FR-002.6 | Policy changes shall take effect without system restart | P0 |

---

## FR-003: Workflow Management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-003.1 | System shall route applications through configurable workflow stages | P0 |
| FR-003.2 | Workflow shall support parallel and sequential stages | P1 |
| FR-003.3 | System shall auto-assign tasks based on rules (round-robin, workload) | P0 |
| FR-003.4 | System shall escalate overdue tasks per SLA configuration | P0 |
| FR-003.5 | Underwriters shall be able to request additional documents | P0 |
| FR-003.6 | System shall support workflow deviation with approval | P1 |

---

## FR-004: Credit Assessment

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-004.1 | System shall fetch credit score from CIBIL, Experian, Equifax, CRIF | P0 |
| FR-004.2 | System shall calculate Debt-to-Income (DTI) ratio | P0 |
| FR-004.3 | System shall calculate EMI/NMI (EMI to Net Monthly Income) ratio | P0 |
| FR-004.4 | System shall apply credit scoring model (internal scorecard) | P1 |
| FR-004.5 | System shall flag negative markers (defaults, write-offs, settlements) | P0 |
| FR-004.6 | System shall verify income via ITR, bank statements, GST returns | P0 |

---

## FR-005: Document Management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-005.1 | System shall accept document uploads (PDF, JPG, PNG) | P0 |
| FR-005.2 | System shall classify documents using AI/OCR | P1 |
| FR-005.3 | System shall extract data from KYC documents (PAN, Aadhaar) | P1 |
| FR-005.4 | System shall verify documents against source (DigiLocker, UIDAI) | P1 |
| FR-005.5 | System shall maintain document version history | P0 |
| FR-005.6 | System shall generate loan documents (sanction letter, agreement) | P0 |

---

## FR-006: Regulatory Compliance (India-Specific)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-006.1 | System shall enforce RBI KYC norms (CKYC integration) | P0 |
| FR-006.2 | System shall register secured assets with CERSAI | P0 |
| FR-006.3 | System shall comply with Fair Practices Code (FPC) | P0 |
| FR-006.4 | System shall calculate interest per RBI guidelines (reducing balance) | P0 |
| FR-006.5 | System shall track Priority Sector Lending (PSL) targets | P1 |
| FR-006.6 | System shall generate RBI regulatory reports | P1 |

---

## FR-007: Disbursement

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-007.1 | System shall support NEFT/RTGS/IMPS disbursement | P0 |
| FR-007.2 | System shall support tranche-based disbursement (home loans) | P1 |
| FR-007.3 | System shall integrate with CBS for account creation | P1 |
| FR-007.4 | System shall generate disbursement memo | P0 |
| FR-007.5 | System shall support pre-disbursement conditions tracking | P0 |
