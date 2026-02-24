# LoanFlow UAT Guide — Sprints 8-11

## 1. Prerequisites & Environment Setup

### 1.1 Prerequisites
- Docker Desktop running
- Java 20 installed (`JAVA_HOME=/Users/rayan/Library/Java/JavaVirtualMachines/openjdk-20.0.1/Contents/Home`)
- Node.js / npm installed (for Angular frontend)
- Stop local Homebrew PostgreSQL: `brew services stop postgresql@16 2>/dev/null; brew services stop postgresql 2>/dev/null`

### 1.2 Start Infrastructure
```bash
export JAVA_HOME=/Users/rayan/Library/Java/JavaVirtualMachines/openjdk-20.0.1/Contents/Home
cd loanflow/infrastructure
docker compose up -d
```
Wait ~120 seconds for Keycloak and ClamAV to become healthy.

### 1.3 Build Backend
```bash
export JAVA_HOME=/Users/rayan/Library/Java/JavaVirtualMachines/openjdk-20.0.1/Contents/Home
cd loanflow/backend
mvn clean install -DskipTests
```

### 1.4 Start All Services
```bash
export JAVA_HOME=/Users/rayan/Library/Java/JavaVirtualMachines/openjdk-20.0.1/Contents/Home
PROJECT="<path-to-loanflow>"

nohup $JAVA_HOME/bin/java -jar "$PROJECT/backend/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar" > /tmp/auth-service.log 2>&1 &
nohup $JAVA_HOME/bin/java -jar "$PROJECT/backend/customer-service/target/customer-service-1.0.0-SNAPSHOT.jar" > /tmp/customer-service.log 2>&1 &
nohup $JAVA_HOME/bin/java -jar "$PROJECT/backend/loan-service/target/loan-service-1.0.0-SNAPSHOT.jar" > /tmp/loan-service.log 2>&1 &
nohup $JAVA_HOME/bin/java -jar "$PROJECT/backend/document-service/target/document-service-1.0.0-SNAPSHOT.jar" > /tmp/document-service.log 2>&1 &
nohup $JAVA_HOME/bin/java -jar "$PROJECT/backend/policy-service/target/policy-service-1.0.0-SNAPSHOT.jar" > /tmp/policy-service.log 2>&1 &
nohup $JAVA_HOME/bin/java -jar "$PROJECT/backend/notification-service/target/notification-service-1.0.0-SNAPSHOT.jar" > /tmp/notification-service.log 2>&1 &
nohup $JAVA_HOME/bin/java -jar "$PROJECT/backend/api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar" > /tmp/api-gateway.log 2>&1 &
```

### 1.5 Start Angular Frontend
```bash
cd loanflow/frontend/loanflow-web
npx ng serve --proxy-config proxy.conf.json
```

### 1.6 Verify Health
All services should return `{"status":"UP"}`:
```bash
for port in 8080 8081 8082 8083 8084 8085 8086; do
  echo "Port $port: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/actuator/health)"
done
```
- Keycloak: http://localhost:8180/health/ready
- MailHog: http://localhost:8025 (email inbox)
- Frontend: http://localhost:4200

### 1.7 Test User Credentials

| User | Password | Role | Use For |
|------|----------|------|---------|
| `officer@loanflow.com` | `officer123` | LOAN_OFFICER | Create apps, upload docs, pull reports |
| `officer2@loanflow.com` | `officer123` | LOAN_OFFICER | Second officer for workload tests |
| `underwriter@loanflow.com` | `underwriter123` | UNDERWRITER | Approve/reject applications |
| `senior.uw@loanflow.com` | `senior123` | SENIOR_UNDERWRITER | High-value approvals |
| `manager@loanflow.com` | `manager123` | BRANCH_MANAGER | Final escalation authority |
| `customer@example.com` | `customer123` | CUSTOMER | Customer portal self-service |
| `admin@loanflow.com` | `admin123` | ADMIN | Full system access |

---

## 2. Test Data Preparation

Before running sprint-specific tests, create base test data:

### 2.1 Create Test Customer
1. Login as `officer@loanflow.com` at http://localhost:4200
2. Navigate to **Customers** in the sidebar
3. Click **New Customer**
4. Fill in:
   - First Name: `Rajesh`, Last Name: `Kumar`
   - Email: `rajesh.kumar@example.com`
   - Mobile: `+919876543210`
   - PAN: `ABCDE1234F`
   - Aadhaar: `123456789012`
   - DOB: `1990-05-15`
   - Address: 123 MG Road, Mumbai, Maharashtra, 400001
5. Click **Save** — note the Customer ID

### 2.2 Create Test Loan Application
1. Navigate to **Loans** → **New Application**
2. Select the customer created above
3. Fill in:
   - Loan Type: `HOME_LOAN`
   - Requested Amount: `5000000` (50 lakhs)
   - Tenure: `240` months
   - Purpose: `Purchase of residential property`
   - Employment Type: `SALARIED`
   - Monthly Income: `150000`
4. Click **Save as Draft** — note the Application ID and Application Number

---

## 3. Sprint 8: Credit Bureau, Income Verification, Document Upload

### TC-S8-01: CIBIL Credit Bureau Pull
| Field | Value |
|-------|-------|
| **User Story** | US-016 |
| **Login As** | `officer@loanflow.com` |
| **Precondition** | Loan application exists in SUBMITTED status (submit the draft first) |

**Steps:**
1. Submit the draft loan application (Loans → open app → Submit)
2. Navigate to **Tasks** → **Task Inbox**
3. Locate the task for the submitted application and click to open **Task Detail**
4. Click the **Credit Info** tab
5. Click **Pull Credit Report**
6. The system uses the customer's PAN number

**Expected Results:**
- [ ] CIBIL score is displayed (e.g., 750)
- [ ] Score factors listed (e.g., "Length of credit history", "Payment history")
- [ ] Account Summary shows: total accounts, outstanding balance, DPD 90+ count, written-off accounts
- [ ] Enquiry history shows recent enquiries with dates and amounts
- [ ] Data source badge shows `SIMULATED` (mock mode)
- [ ] Bureau Pull Time shows current timestamp

---

### TC-S8-02: CIBIL Cached Report
| Field | Value |
|-------|-------|
| **Precondition** | TC-S8-01 completed for the same application |

**Steps:**
1. From the same Task Detail, click **Pull Credit Report** again

**Expected Results:**
- [ ] Report loads faster (from Redis cache, 24h TTL)
- [ ] Data source badge shows `CACHED`
- [ ] All data matches the first pull exactly
- [ ] Bureau Pull Time shows the original pull time

---

### TC-S8-03: Income Verification Report
| Field | Value |
|-------|-------|
| **User Story** | US-017 |
| **Login As** | `officer@loanflow.com` |
| **Precondition** | Task Detail page is open |

**Steps:**
1. On the Task Detail page, click the **Credit Info** tab
2. Click **Pull Income Data**
3. Review the multi-tab results

**Expected Results:**
- [ ] **Verified Monthly Income** card shows a currency amount (INR)
- [ ] **DTI Ratio** card shows a percentage (green <40%, amber 40-50%, red >50%)
- [ ] **Consistency Score** card shows 0-100 value
- [ ] **ITR Data** panel shows: Assessment Year, ITR Form Type, Gross Income, Salary breakdown
- [ ] **GST Data** panel shows: GSTIN, Annual Turnover, Compliance Rating, Filing Count
- [ ] **Bank Statement Data** panel shows: Months Analyzed, Avg Monthly Balance, Avg Credits, Bounce Count
- [ ] Data source badge shows `SIMULATED`

---

### TC-S8-04: Document Upload with Drag-Drop & Virus Scan
| Field | Value |
|-------|-------|
| **User Story** | US-020 |
| **Login As** | `officer@loanflow.com` |
| **Precondition** | Loan application exists |

**Steps:**
1. Navigate to **Documents** → **Upload Document**
2. Search and select the loan application
3. Select Category: `KYC`, Type: `PAN_CARD`
4. **Drag and drop** a valid PDF file (under 10MB) onto the upload zone
5. Observe upload progress

**Expected Results:**
- [ ] Drag-over zone highlights when file hovers over it
- [ ] File name, size, and type shown before upload
- [ ] Progress bar shows: **"Scanning for viruses..."** → **"Uploading..."**
- [ ] Upload completes successfully
- [ ] Document appears in document list with status `UPLOADED`
- [ ] Success notification shown

---

### TC-S8-05: Oversized File Rejection
| Field | Value |
|-------|-------|
| **Precondition** | Upload form is open |

**Steps:**
1. Drag and drop a file **larger than 10MB** onto the upload zone

**Expected Results:**
- [ ] File is rejected **before** upload begins (client-side validation)
- [ ] Error message: file exceeds 10MB size limit
- [ ] No network request is made

---

### TC-S8-06: Invalid File Type Rejection
**Steps:**
1. Attempt to upload a file with `.exe`, `.zip`, or other disallowed extension

**Expected Results:**
- [ ] File is rejected **before** upload begins
- [ ] Error message lists allowed types: PDF, JPG, JPEG, PNG, TIFF, DOC, DOCX
- [ ] No network request is made

---

## 4. Sprint 9: Document Verification, OCR, Sanction Letter

### TC-S9-01: Document Verification — Approve
| Field | Value |
|-------|-------|
| **User Story** | US-021 |
| **Login As** | `officer@loanflow.com` |
| **Precondition** | Documents uploaded for a loan application (status: UPLOADED) |

**Steps:**
1. Open Task Detail for an application with uploaded documents
2. Click the **Documents** tab (DocumentPanelComponent)
3. Locate a document with `UPLOADED` status
4. Click the **green approve** (check) button on that document
5. Optionally add verification remarks

**Expected Results:**
- [ ] Document status changes from `UPLOADED` to `VERIFIED`
- [ ] Green "Verified" chip/badge displayed
- [ ] Document completeness percentage updates
- [ ] Success notification shown

---

### TC-S9-02: Document Verification — Reject
**Steps:**
1. Same as TC-S9-01 but click the **red reject** (X) button
2. Enter rejection reason

**Expected Results:**
- [ ] Document status changes to `REJECTED`
- [ ] Red "Rejected" chip/badge displayed
- [ ] Rejection reason is saved and visible

---

### TC-S9-03: Document Completeness Check
| Field | Value |
|-------|-------|
| **Precondition** | Loan application of type HOME_LOAN with some documents uploaded |

**Steps:**
1. Open Task Detail → Documents tab
2. Review the completeness indicator

**Expected Results:**
- [ ] Completeness percentage is displayed
- [ ] Required document types for HOME_LOAN are listed
- [ ] Each required type shows upload/verification status
- [ ] Missing required documents are highlighted

---

### TC-S9-04: OCR Data Extraction — PAN Card
| Field | Value |
|-------|-------|
| **User Story** | US-022 |
| **Precondition** | A PAN_CARD PDF/image has been uploaded |

**Steps:**
1. Navigate to the uploaded PAN_CARD document
2. Look for the **ExtractionReviewComponent** showing extracted data

**Expected Results:**
- [ ] Key-value pairs are displayed (e.g., Name, PAN Number, Date of Birth)
- [ ] Extraction status shows one of: `SUCCESS` (green), `PARTIAL` (amber), `FAILED` (red)
- [ ] PAN number follows ABCDE1234F pattern if extracted

---

### TC-S9-05: OCR Extraction Review & Correction
| Field | Value |
|-------|-------|
| **Precondition** | TC-S9-04 — extraction data visible |

**Steps:**
1. Click on an extracted field to edit it (e.g., correct a misspelled name)
2. Modify the value
3. Click **Save Corrections**

**Expected Results:**
- [ ] Fields are editable (inline text inputs)
- [ ] After saving: success notification shown
- [ ] Extraction status changes to `REVIEWED` (blue)
- [ ] Re-opening shows corrected values persisted

---

### TC-S9-06: Sanction Letter PDF Generation
| Field | Value |
|-------|-------|
| **User Story** | US-023 |
| **Login As** | `officer@loanflow.com` |
| **Precondition** | Loan application has been **APPROVED** (complete the full approve workflow first) |

**Steps:**
1. Navigate to **Loans** → click on the approved loan
2. On the Loan Detail page, locate the **Generate Sanction Letter** button
3. Click **Generate Sanction Letter**

**Expected Results:**
- [ ] PDF file downloads to browser
- [ ] PDF contains: application number, customer name, approved amount, interest rate, tenure, EMI
- [ ] PDF has bank letterhead/formatting (Flying Saucer + Thymeleaf)
- [ ] Terms and conditions section present

---

## 5. Sprint 10: e-KYC, Audit Trail, Notifications

### TC-S10-01: e-KYC Initiate OTP
| Field | Value |
|-------|-------|
| **User Story** | US-029 |
| **Login As** | `officer@loanflow.com` |
| **Precondition** | Task Detail open, customer has Aadhaar number |

**Steps:**
1. Open Task Detail page
2. Click the **e-KYC** tab (EkycPanelComponent)
3. Panel should be in `IDLE` state
4. Enter 12-digit Aadhaar number: `123456789012`
5. Click **Initiate e-KYC**

**Expected Results:**
- [ ] State transitions: `IDLE` → `INITIATING` (spinner) → `OTP_INPUT`
- [ ] OTP input field appears
- [ ] Masked mobile number displayed (e.g., `XXXXXX7890`)
- [ ] Transaction reference stored internally

---

### TC-S10-02: e-KYC Verify OTP
| Field | Value |
|-------|-------|
| **Precondition** | TC-S10-01 completed, OTP input visible |

**Steps:**
1. Enter 6-digit OTP (mock: `123456`)
2. Click **Verify OTP**

**Expected Results:**
- [ ] State transitions: `OTP_INPUT` → `VERIFYING` → `VERIFIED`
- [ ] Green "Verified" badge appears
- [ ] Demographic data displayed: Name, Date of Birth, Gender
- [ ] Address details: street, city, state, PIN code
- [ ] CKYC number displayed (unique KYC identifier)
- [ ] Customer KYC status updated in the system

---

### TC-S10-03: e-KYC Invalid Aadhaar
**Steps:**
1. Enter a non-12-digit Aadhaar (e.g., `12345`)
2. Click **Initiate e-KYC**

**Expected Results:**
- [ ] Validation error: "Please enter a valid 12-digit Aadhaar number"
- [ ] No API call is made
- [ ] State remains `IDLE`

---

### TC-S10-04: Audit Trail — View Timeline
| Field | Value |
|-------|-------|
| **User Story** | US-030 |
| **Login As** | `officer@loanflow.com` |
| **Precondition** | Loan application has been through multiple status changes |

**Steps:**
1. Navigate to **Loans** → open a loan with workflow history
2. Click the **Audit Trail** tab (AuditTimelineComponent)

**Expected Results:**
- [ ] Timeline loads with chronological events
- [ ] Each event shows: colored icon, event type label, timestamp, performing user name/role
- [ ] Event types include: `APPLICATION_CREATED`, `APPLICATION_SUBMITTED`, `STATUS_CHANGED`, `DOCUMENT_UPLOADED`, `DOCUMENT_VERIFIED`, `KYC_VERIFIED`
- [ ] Color coding: green (approvals/verifications), red (rejections), blue (submissions), orange (updates)
- [ ] Relative timestamps shown (e.g., "2 hours ago")

---

### TC-S10-05: Audit Trail — Filter by Event Type
| Field | Value |
|-------|-------|
| **Precondition** | TC-S10-04 — audit timeline visible |

**Steps:**
1. Use the event type filter dropdown
2. Select `STATUS_CHANGED`

**Expected Results:**
- [ ] Only status change events are displayed
- [ ] Event count updates to reflect filtered results
- [ ] Clearing the filter restores all events

---

### TC-S10-06: Audit Trail — Expand Event Details
**Steps:**
1. Click on an audit event card to expand it

**Expected Results:**
- [ ] **Before/after state** is shown for state-change events
- [ ] For `STATUS_CHANGED`: previous status and new status visible
- [ ] For `DOCUMENT_VERIFIED`: document ID, type, and verification result shown
- [ ] Performing user details: name, role, user ID

---

### TC-S10-07: Email Notification — Application Submitted
| Field | Value |
|-------|-------|
| **User Story** | US-031 |
| **Precondition** | MailHog running at http://localhost:8025, notification-service running |

**Steps:**
1. Create and submit a new loan application via the frontend
2. Open **MailHog** web UI at http://localhost:8025

**Expected Results:**
- [ ] An email appears in MailHog inbox
- [ ] Subject indicates application submission (contains application number)
- [ ] Body uses Thymeleaf HTML template with LoanFlow branding
- [ ] From: `noreply@loanflow.com`
- [ ] To: customer's email address

---

### TC-S10-08: Email Notification — Application Approved
| Field | Value |
|-------|-------|
| **Login As** | `underwriter@loanflow.com` (to approve) |

**Steps:**
1. Approve a loan application through the Task Inbox workflow
2. Check MailHog at http://localhost:8025

**Expected Results:**
- [ ] Approval notification email appears in MailHog
- [ ] Subject indicates approval
- [ ] Body contains: approved amount, interest rate, loan details
- [ ] Uses `application-approved.html` template

---

### TC-S10-09: Email Notification — Application Rejected
**Steps:**
1. Reject a loan application through the Task Inbox workflow (provide reason)
2. Check MailHog

**Expected Results:**
- [ ] Rejection notification email appears in MailHog
- [ ] Subject indicates rejection
- [ ] Body contains rejection reason
- [ ] Uses `application-rejected.html` template

---

## 6. Sprint 11: API Gateway

### TC-S11-01: API Gateway Routing Verification
| Field | Value |
|-------|-------|
| **User Story** | US-033 |
| **Precondition** | API Gateway running on port 8080 |

**Steps:**
1. Obtain a JWT token:
```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/loanflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=loanflow-web&grant_type=password&username=officer@loanflow.com&password=officer123" \
  | jq -r '.access_token')
```
2. Call APIs through the gateway (port 8080):
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/customers
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/loans
curl http://localhost:8080/actuator/health
```

**Expected Results:**
- [ ] Customer list returns successfully through gateway
- [ ] Loan list returns successfully through gateway
- [ ] Gateway health returns `UP`
- [ ] Responses match direct service calls (ports 8081/8082)
- [ ] Unauthenticated requests to `/api/**` return 401

---

### TC-S11-02: API Gateway Health & Actuator
**Steps:**
1. Access gateway actuator endpoints:
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
```

**Expected Results:**
- [ ] Health endpoint returns `{"status":"UP"}`
- [ ] Actuator endpoints accessible without authentication

---

## 7. End-to-End Scenario: Complete Loan Lifecycle

**E2E-01** exercises ALL Sprint 8-11 features in a single loan lifecycle.

### Phase 1: Customer & Application Setup
| Step | Action | Login As | Expected |
|------|--------|----------|----------|
| 1 | Create customer: Rajesh Kumar, PAN: ABCDE1234F, Aadhaar: 123456789012 | officer | Customer created with ACTIVE status |
| 2 | Create HOME_LOAN: 50 lakhs, 240 months | officer | Draft application created with number |

### Phase 2: Document Upload (Sprint 8 — US-020)
| Step | Action | Expected |
|------|--------|----------|
| 3 | Upload PAN_CARD (drag-drop PDF) | Virus scan progress → UPLOADED |
| 4 | Upload SALARY_SLIP | Virus scan progress → UPLOADED |
| 5 | Upload PROPERTY_DEED | Virus scan progress → UPLOADED |
| 6 | Try uploading >10MB file | Rejected before upload |

### Phase 3: Submit & Notifications (Sprint 10 — US-031)
| Step | Action | Expected |
|------|--------|----------|
| 7 | Submit the application | Status → SUBMITTED/DOCUMENT_VERIFICATION |
| 8 | Check MailHog (http://localhost:8025) | Submission email received |

### Phase 4: e-KYC Verification (Sprint 10 — US-029)
| Step | Action | Login As | Expected |
|------|--------|----------|----------|
| 9 | Open task from Task Inbox → e-KYC tab | officer | Panel in IDLE state |
| 10 | Enter Aadhaar 123456789012 → Initiate | officer | OTP input appears |
| 11 | Enter mock OTP → Verify | officer | VERIFIED with demographic data |

### Phase 5: Credit Assessment (Sprint 8 — US-016, US-017)
| Step | Action | Expected |
|------|--------|----------|
| 12 | Task Detail → Credit tab → Pull Credit Report | CIBIL score + factors + accounts |
| 13 | Pull Credit Report again (same PAN) | Badge shows CACHED |
| 14 | Pull Income Data | Monthly income, DTI ratio, ITR/GST/Bank data |

### Phase 6: Document Verification & OCR (Sprint 9 — US-021, US-022)
| Step | Action | Expected |
|------|--------|----------|
| 15 | Task Detail → Documents tab | Document list with completeness % |
| 16 | View PAN_CARD → check OCR extraction | Extracted key-value pairs (name, PAN) |
| 17 | Edit extracted field → Save Corrections | Status → REVIEWED |
| 18 | Approve PAN_CARD document | Status → VERIFIED |
| 19 | Approve SALARY_SLIP and PROPERTY_DEED | All docs VERIFIED |

### Phase 7: Loan Approval (Multi-role)
| Step | Action | Login As | Expected |
|------|--------|----------|----------|
| 20 | Transition to CREDIT_CHECK then UNDERWRITING | officer | Status advances |
| 21 | Open task → Decision Panel → Approve (amount: 4500000, rate: 8.5%) | underwriter | APPROVED status |
| 22 | Check MailHog | — | Approval email received |

### Phase 8: Sanction Letter (Sprint 9 — US-023)
| Step | Action | Login As | Expected |
|------|--------|----------|----------|
| 23 | Loans → approved loan → Generate Sanction Letter | officer | PDF downloads |
| 24 | Open PDF | — | Contains loan details, amount, rate, terms |

### Phase 9: Audit Trail (Sprint 10 — US-030)
| Step | Action | Expected |
|------|--------|----------|
| 25 | Loan Detail → Audit Trail tab | Full event timeline |
| 26 | Verify events: CREATED, SUBMITTED, DOC_UPLOADED, DOC_VERIFIED, KYC_VERIFIED, STATUS_CHANGED, APPROVED | All present in timeline |
| 27 | Filter by STATUS_CHANGED | Only status events shown |
| 28 | Expand an event | Before/after state visible |

### Phase 10: API Gateway (Sprint 11 — US-033)
| Step | Action | Expected |
|------|--------|----------|
| 29 | curl through :8080 with JWT token | Same responses as direct service |
| 30 | curl :8080/actuator/health | Returns UP |

### E2E Result
- [ ] **PASS** — All 30 steps completed successfully
- [ ] **FAIL** — Note failed step number and details below:

**Notes:**
_____________________________________________

---

## 8. Troubleshooting

| Issue | Solution |
|-------|----------|
| Services won't start | Check logs: `tail -50 /tmp/<service-name>.log` |
| DB connection failed | Verify Docker: `docker compose ps` — PostgreSQL/MongoDB must be healthy |
| Keycloak 403 on login | Run: `docker exec loanflow-keycloak /opt/keycloak/bin/kcadm.sh update realms/loanflow -s sslRequired=NONE` |
| Task Inbox empty | Keycloak user UUIDs must match `application.yml` — see CLAUDE.md |
| ClamAV timeout on upload | ClamAV needs ~120s to init virus DB on first start — wait and retry |
| No emails in MailHog | Verify notification-service running: `curl localhost:8084/actuator/health`. Check RabbitMQ: http://localhost:15672 |
| Frontend proxy errors | Ensure all services running before `ng serve`. Check proxy.conf.json has all routes |
| CIBIL/Income shows SIMULATED | Expected — mock API clients are used in dev/UAT mode |
| OCR extraction empty | Ensure uploaded file is a real PDF/image with text — blank or corrupt files won't extract |

---

## 9. Service URLs Quick Reference

| Service | Direct URL | Via Gateway |
|---------|-----------|-------------|
| Frontend | http://localhost:4200 | — |
| API Gateway | http://localhost:8080 | — |
| Auth Service | http://localhost:8085 | http://localhost:8080/api/v1/auth |
| Customer Service | http://localhost:8082 | http://localhost:8080/api/v1/customers |
| Loan Service | http://localhost:8081 | http://localhost:8080/api/v1/loans |
| Document Service | http://localhost:8083 | http://localhost:8080/api/v1/documents |
| Policy Service | http://localhost:8086 | http://localhost:8080/api/v1/policies |
| Notification Service | http://localhost:8084 | http://localhost:8080/api/v1/notifications |
| Keycloak Admin | http://localhost:8180 | — |
| MailHog | http://localhost:8025 | — |
| RabbitMQ Admin | http://localhost:15672 | — |
| MinIO Console | http://localhost:9001 | — |

---

## 10. Test Summary Checklist

### Sprint 8 (6 tests)
- [ ] TC-S8-01: CIBIL Credit Bureau Pull
- [ ] TC-S8-02: CIBIL Cached Report
- [ ] TC-S8-03: Income Verification Report
- [ ] TC-S8-04: Document Upload with Virus Scan
- [ ] TC-S8-05: Oversized File Rejection
- [ ] TC-S8-06: Invalid File Type Rejection

### Sprint 9 (6 tests)
- [ ] TC-S9-01: Document Verify — Approve
- [ ] TC-S9-02: Document Verify — Reject
- [ ] TC-S9-03: Document Completeness Check
- [ ] TC-S9-04: OCR Data Extraction
- [ ] TC-S9-05: OCR Review & Correction
- [ ] TC-S9-06: Sanction Letter PDF Generation

### Sprint 10 (9 tests)
- [ ] TC-S10-01: e-KYC Initiate OTP
- [ ] TC-S10-02: e-KYC Verify OTP
- [ ] TC-S10-03: e-KYC Invalid Aadhaar
- [ ] TC-S10-04: Audit Trail View
- [ ] TC-S10-05: Audit Trail Filter
- [ ] TC-S10-06: Audit Trail Expand Details
- [ ] TC-S10-07: Email — Application Submitted
- [ ] TC-S10-08: Email — Application Approved
- [ ] TC-S10-09: Email — Application Rejected

### Sprint 11 (2 tests)
- [ ] TC-S11-01: API Gateway Routing
- [ ] TC-S11-02: API Gateway Health

### End-to-End (1 test)
- [ ] E2E-01: Complete Loan Lifecycle

**Total: 24 test cases**

| Sprint | Tests | Pass | Fail |
|--------|-------|------|------|
| Sprint 8 | 6 | ___ | ___ |
| Sprint 9 | 6 | ___ | ___ |
| Sprint 10 | 9 | ___ | ___ |
| Sprint 11 | 2 | ___ | ___ |
| E2E | 1 | ___ | ___ |
| **Total** | **24** | ___ | ___ |

**UAT Sign-off:**
- Tester: _______________
- Date: _______________
- Result: PASS / FAIL
- Notes: _______________
