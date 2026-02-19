# LoanFlow UAT Testing Guide

## Prerequisites
- Docker containers running (PostgreSQL, MongoDB, Keycloak, MinIO, RabbitMQ)
- Java 20 installed
- Maven installed

## Quick Start

```bash
# 1. Start infrastructure
cd loanflow/docker
docker-compose up -d

# 2. Start services (run each in separate terminal)
cd backend/loan-service && mvn spring-boot:run
cd backend/customer-service && mvn spring-boot:run
cd backend/document-service && mvn spring-boot:run
```

---

## Epic 1: Customer Management (customer-service)

### User Story 1.1: Create Customer
**Endpoint:** `POST /api/v1/customers`

```bash
# Test: Create a new customer with Indian KYC data
curl -X POST http://localhost:8082/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "email": "rajesh.kumar@example.com",
    "mobile": "+919876543210",
    "dateOfBirth": "1990-05-15",
    "gender": "MALE",
    "panNumber": "ABCDE1234F",
    "aadhaarNumber": "123456789012",
    "address": {
      "addressLine1": "123 MG Road",
      "city": "Mumbai",
      "state": "Maharashtra",
      "pincode": "400001",
      "country": "India"
    }
  }'
```

**Expected Response:**
- Status: 201 Created
- Customer ID generated
- Status: ACTIVE

**Validation Tests:**
| Test Case | Input | Expected |
|-----------|-------|----------|
| Invalid PAN | "ABC123" | 400 - Invalid PAN format |
| Invalid Aadhaar | "12345" | 400 - Aadhaar must be 12 digits |
| Invalid Mobile | "12345" | 400 - Invalid mobile format |
| Duplicate PAN | Same PAN twice | 409 - PAN already exists |

---

### User Story 1.2: Get Customer
**Endpoint:** `GET /api/v1/customers/{id}`

```bash
# Test: Retrieve customer by ID
curl http://localhost:8082/api/v1/customers/{customer-id}
```

**Expected:** Customer details with masked PAN (AB******4F)

---

### User Story 1.3: KYC Verification
**Endpoint:** `PUT /api/v1/customers/{id}/verify-kyc`

```bash
# Test: Verify customer KYC
curl -X PUT http://localhost:8082/api/v1/customers/{id}/verify-kyc \
  -H "Content-Type: application/json" \
  -d '{
    "aadhaarVerified": true,
    "panVerified": true,
    "verifiedBy": "officer-123"
  }'
```

**Expected:** KYC status updated to VERIFIED

---

## Epic 2: Loan Application (loan-service)

### User Story 2.1: Create Loan Application
**Endpoint:** `POST /api/v1/loans`

```bash
# Test: Create home loan application
curl -X POST http://localhost:8081/api/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{customer-id-from-above}",
    "loanType": "HOME_LOAN",
    "requestedAmount": 5000000,
    "tenureMonths": 240,
    "purpose": "Purchase of residential property",
    "propertyValue": 7500000,
    "employmentType": "SALARIED",
    "monthlyIncome": 150000
  }'
```

**Expected Response:**
- Status: 201 Created
- Application number: LOAN-2024-XXXXXX
- Status: DRAFT

**Loan Types to Test:**
| Type | Min Amount | Max Tenure |
|------|------------|------------|
| HOME_LOAN | ₹5,00,000 | 360 months |
| PERSONAL_LOAN | ₹50,000 | 60 months |
| VEHICLE_LOAN | ₹1,00,000 | 84 months |
| BUSINESS_LOAN | ₹5,00,000 | 120 months |
| LAP | ₹10,00,000 | 180 months |

---

### User Story 2.2: Submit Application
**Endpoint:** `PUT /api/v1/loans/{id}/submit`

```bash
# Test: Submit application for review
curl -X PUT http://localhost:8081/api/v1/loans/{id}/submit
```

**Expected:** Status changes from DRAFT → SUBMITTED

---

### User Story 2.3: Application Workflow
**Status Flow:**
```
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED → DISBURSED
```

```bash
# Move to review
curl -X PUT http://localhost:8081/api/v1/loans/{id}/review

# Approve (requires UNDERWRITER role)
curl -X PUT http://localhost:8081/api/v1/loans/{id}/approve \
  -d '{"approvedAmount": 4500000, "interestRate": 8.5, "remarks": "Approved"}'

# Reject
curl -X PUT http://localhost:8081/api/v1/loans/{id}/reject \
  -d '{"reason": "Insufficient income"}'
```

---

## Epic 3: Document Management (document-service)

### User Story 3.1: Upload Document
**Endpoint:** `POST /api/v1/documents`

```bash
# Test: Upload PAN card
curl -X POST http://localhost:8083/api/v1/documents \
  -F "file=@/path/to/pan_card.pdf" \
  -F 'request={"applicationId":"{loan-id}","customerId":"{customer-id}","documentType":"PAN_CARD"}'
```

**Expected:**
- Status: 201 Created
- Document number: DOC-2024-XXXXXX
- Status: UPLOADED
- Stored in MinIO

**Document Types to Test:**
| Category | Types |
|----------|-------|
| KYC | PAN_CARD, AADHAAR_CARD, PASSPORT, VOTER_ID |
| Income | SALARY_SLIP, FORM_16, ITR |
| Financial | BANK_STATEMENT, EXISTING_LOAN_STATEMENT |
| Property | PROPERTY_DEED, SALE_AGREEMENT |

---

### User Story 3.2: Verify Document
**Endpoint:** `POST /api/v1/documents/{id}/verify`

```bash
# Test: Verify uploaded document
curl -X POST http://localhost:8083/api/v1/documents/{id}/verify \
  -H "Content-Type: application/json" \
  -d '{
    "verifierId": "underwriter-123",
    "approved": true,
    "remarks": "Document is valid and clear"
  }'
```

**Expected:** Status changes to VERIFIED

---

### User Story 3.3: Download Document
**Endpoint:** `GET /api/v1/documents/{id}/download-url`

```bash
# Get presigned download URL
curl http://localhost:8083/api/v1/documents/{id}/download-url
```

**Expected:** MinIO presigned URL (valid for 15 minutes)

---

## End-to-End Test Scenario

### Complete Loan Application Flow

```bash
# Step 1: Create Customer
CUSTOMER_ID=$(curl -s -X POST http://localhost:8082/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","mobile":"+919876543210","panNumber":"ABCDE1234F"}' \
  | jq -r '.data.id')

echo "Customer ID: $CUSTOMER_ID"

# Step 2: Create Loan Application
LOAN_ID=$(curl -s -X POST http://localhost:8081/api/v1/loans \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":\"$CUSTOMER_ID\",\"loanType\":\"PERSONAL_LOAN\",\"requestedAmount\":500000,\"tenureMonths\":36}" \
  | jq -r '.data.id')

echo "Loan ID: $LOAN_ID"

# Step 3: Upload Documents
curl -X POST http://localhost:8083/api/v1/documents \
  -F "file=@pan.pdf" \
  -F "request={\"applicationId\":\"$LOAN_ID\",\"customerId\":\"$CUSTOMER_ID\",\"documentType\":\"PAN_CARD\"}"

# Step 4: Submit Application
curl -X PUT http://localhost:8081/api/v1/loans/$LOAN_ID/submit

# Step 5: Check Status
curl http://localhost:8081/api/v1/loans/$LOAN_ID
```

---

## Database Verification

### PostgreSQL
```bash
# Connect to database
docker exec -it loanflow-postgres psql -U loanflow -d loan_db

# Check loan applications
SELECT application_number, loan_type, status, requested_amount FROM application.loan_applications;

# Check customers
\c customer_db
SELECT customer_number, first_name, pan_number, status FROM identity.customers;
```

### MongoDB
```bash
# Connect to MongoDB
docker exec -it loanflow-mongodb mongosh

# Check documents
use document_db
db.documents.find().pretty()
```

### MinIO
- Access: http://localhost:9001
- Login: minioadmin / minioadmin
- Check `loanflow-documents` bucket

---

## Swagger UI Access

Once services are running:
- loan-service: http://localhost:8081/swagger-ui.html
- customer-service: http://localhost:8082/swagger-ui.html
- document-service: http://localhost:8083/swagger-ui.html

---

## Test Checklist

### Customer Service ✅
- [ ] Create customer with valid Indian KYC
- [ ] Validate PAN format (ABCDE1234F pattern)
- [ ] Validate Aadhaar (12 digits)
- [ ] Validate mobile (+91 format)
- [ ] Prevent duplicate PAN/Aadhaar
- [ ] KYC verification flow
- [ ] PAN masking in responses

### Loan Service ✅
- [ ] Create loan application
- [ ] Generate unique application number
- [ ] Validate loan type specific rules
- [ ] Status workflow (DRAFT → SUBMITTED → APPROVED)
- [ ] Link customer to application

### Document Service ✅
- [ ] Upload PDF/Image documents
- [ ] Validate file type (PDF, JPEG, PNG only)
- [ ] Validate file size (max 10MB)
- [ ] Auto-categorize documents
- [ ] Document verification workflow
- [ ] Generate download URLs
- [ ] Track document expiry
