# External Integration Specifications

## LoanFlow - Loan Origination System

---

## 1. CIBIL (TransUnion) Integration

### Overview
Credit bureau integration for fetching credit scores and reports.

### API Details
| Item | Value |
|------|-------|
| Base URL (UAT) | `https://uat-api.cibil.com/v2` |
| Base URL (PROD) | `https://api.cibil.com/v2` |
| Auth | mTLS + API Key |
| Format | XML (TUEF) |
| Timeout | 30 seconds |

### Endpoints

#### 1.1 Consumer Credit Report
```
POST /consumer/credit-report
Content-Type: application/xml
X-API-Key: {api_key}
```

**Request:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<TUEF_REQUEST>
    <HEADER>
        <MEMBER_REF_NO>{member_code}</MEMBER_REF_NO>
        <SECURITY_CODE>{security_code}</SECURITY_CODE>
        <PRODUCT_TYPE>CREDIT_REPORT</PRODUCT_TYPE>
        <INQUIRY_PURPOSE>01</INQUIRY_PURPOSE> <!-- 01=New Credit -->
    </HEADER>
    <INQUIRY>
        <NAME>
            <FIRST_NAME>{first_name}</FIRST_NAME>
            <LAST_NAME>{last_name}</LAST_NAME>
        </NAME>
        <ID_TYPE>01</ID_TYPE> <!-- 01=PAN -->
        <ID_NUMBER>{pan_number}</ID_NUMBER>
        <DATE_OF_BIRTH>{dob_ddmmyyyy}</DATE_OF_BIRTH>
        <GENDER>{M/F}</GENDER>
        <TELEPHONE_NUMBER>{mobile}</TELEPHONE_NUMBER>
        <INQUIRY_AMOUNT>{loan_amount}</INQUIRY_AMOUNT>
    </INQUIRY>
</TUEF_REQUEST>
```

**Response:**
```xml
<TUEF_RESPONSE>
    <HEADER>
        <REPORT_DATE>20250216</REPORT_DATE>
        <CONTROL_NUMBER>{control_number}</CONTROL_NUMBER>
    </HEADER>
    <SCORE>
        <SCORE_VALUE>750</SCORE_VALUE>
        <SCORE_VERSION>TransUnion CIBIL Score 2.0</SCORE_VERSION>
        <SCORE_FACTORS>
            <FACTOR>High credit utilization</FACTOR>
        </SCORE_FACTORS>
    </SCORE>
    <ACCOUNTS>
        <ACCOUNT>
            <ACCOUNT_TYPE>Credit Card</ACCOUNT_TYPE>
            <MEMBER_NAME>HDFC Bank</MEMBER_NAME>
            <CURRENT_BALANCE>45000</CURRENT_BALANCE>
            <AMOUNT_OVERDUE>0</AMOUNT_OVERDUE>
            <DPD_CURRENT>000</DPD_CURRENT>
        </ACCOUNT>
    </ACCOUNTS>
    <INQUIRIES>
        <INQUIRY>
            <DATE>20250110</DATE>
            <MEMBER_NAME>ICICI Bank</MEMBER_NAME>
            <PURPOSE>Personal Loan</PURPOSE>
        </INQUIRY>
    </INQUIRIES>
</TUEF_RESPONSE>
```

### Error Codes
| Code | Description |
|------|-------------|
| 001 | Invalid PAN format |
| 002 | No records found |
| 003 | Authentication failed |
| 004 | Rate limit exceeded |
| 005 | Service unavailable |

---

## 2. UIDAI e-KYC Integration

### Overview
Aadhaar-based electronic KYC verification.

### API Details
| Item | Value |
|------|-------|
| Base URL (UAT) | `https://stage1.uidai.gov.in/` |
| Base URL (PROD) | `https://auth.uidai.gov.in/` |
| Auth | Digital Signature + License Key |
| Format | XML (signed, encrypted) |
| Timeout | 30 seconds |

### Endpoints

#### 2.1 OTP Generation
```
POST /otp/{uidai_version}/otp
Content-Type: application/xml
```

**Request:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Otp xmlns="http://www.uidai.gov.in/auth/otp/1.0"
     uid="{aadhaar_last_4_digits}"
     tid="{terminal_id}"
     ac="{aua_code}"
     sa="{sub_aua_code}"
     ver="2.5"
     txn="{transaction_id}"
     appid="LOANFLOW"
     type="A">
    <Opts ch="01"/> <!-- 01=Mobile OTP -->
</Otp>
```

#### 2.2 e-KYC Request
```
POST /kyc/{uidai_version}/kyc
Content-Type: application/xml
```

**Request (Post OTP Validation):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Kyc xmlns="http://www.uidai.gov.in/kyc/1.0"
     ver="2.5"
     ra="O"
     rc="Y"
     lr="N"
     de="Y">
    <Rad>
        <Pid ts="{timestamp}" ver="2.0">
            <Demo>
                <Pi ms="E" name="{name}"/>
            </Demo>
            <Pv otp="{otp_value}"/>
        </Pid>
    </Rad>
</Kyc>
```

**Response (Decrypted):**
```xml
<KycRes code="0" ret="Y" ts="{timestamp}" txn="{txn_id}">
    <UidData uid="{masked_aadhaar}">
        <Poi dob="1990-05-15" gender="M" name="JOHN DOE"/>
        <Poa co="S/O Richard Doe"
             house="123"
             street="MG Road"
             lm="Near Park"
             loc="Andheri East"
             dist="Mumbai"
             state="Maharashtra"
             pc="400069"
             country="India"/>
        <Pht>{base64_photo}</Pht>
    </UidData>
</KycRes>
```

### Response Codes
| Code | Description |
|------|-------------|
| 0 | Success |
| 100 | Invalid Aadhaar |
| 110 | OTP Expired |
| 300 | Technical error |
| 940 | Auth transaction limit exceeded |

---

## 3. CERSAI Integration

### Overview
Central Registry of Securitisation Asset Reconstruction and Security Interest.

### API Details
| Item | Value |
|------|-------|
| Base URL (UAT) | `https://uat.cersai.org.in/api/v1` |
| Base URL (PROD) | `https://api.cersai.org.in/api/v1` |
| Auth | OAuth 2.0 (Client Credentials) |
| Format | JSON |
| Timeout | 60 seconds |

### Endpoints

#### 3.1 Search Existing Charges
```
POST /search/charges
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request:**
```json
{
  "searchType": "PROPERTY",
  "searchParams": {
    "propertyType": "IMMOVABLE",
    "propertySubType": "RESIDENTIAL",
    "propertyAddress": {
      "plotNo": "123",
      "locality": "Andheri East",
      "city": "Mumbai",
      "state": "MAHARASHTRA",
      "pincode": "400069"
    },
    "surveyNo": "456/A"
  },
  "applicantDetails": {
    "pan": "ABCDE1234F",
    "name": "JOHN DOE"
  }
}
```

**Response:**
```json
{
  "searchId": "SRCH-2025-001234",
  "searchDate": "2025-02-16T10:30:00Z",
  "status": "COMPLETED",
  "results": [
    {
      "chargeId": "SI-2023-XXXXXX",
      "chargeType": "MORTGAGE",
      "securedCreditor": "HDFC Bank Ltd",
      "principalAmount": 5000000,
      "chargeCreationDate": "2023-05-15",
      "chargeStatus": "ACTIVE"
    }
  ],
  "encumbranceStatus": "ENCUMBERED",
  "noOfCharges": 1
}
```

#### 3.2 Register New Charge
```
POST /charge/register
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request:**
```json
{
  "transactionType": "SECURITY_INTEREST_CREATION",
  "securedCreditorDetails": {
    "name": "LoanFlow Bank",
    "registrationNumber": "CERSAI-SC-12345",
    "branchCode": "MUM001"
  },
  "borrowerDetails": {
    "type": "INDIVIDUAL",
    "name": "JOHN DOE",
    "pan": "ABCDE1234F",
    "aadhaar": "XXXX-XXXX-1234",
    "address": {
      "line1": "123 MG Road",
      "city": "Mumbai",
      "state": "MAHARASHTRA",
      "pincode": "400069"
    }
  },
  "assetDetails": {
    "assetType": "IMMOVABLE_PROPERTY",
    "propertyType": "RESIDENTIAL",
    "propertyAddress": {
      "plotNo": "456",
      "surveyNo": "789/B",
      "locality": "Bandra West",
      "city": "Mumbai",
      "state": "MAHARASHTRA",
      "pincode": "400050"
    },
    "marketValue": 15000000
  },
  "loanDetails": {
    "sanctionAmount": 10000000,
    "disbursementDate": "2025-02-16",
    "tenure": 240,
    "interestRate": 8.5
  }
}
```

**Response:**
```json
{
  "status": "REGISTERED",
  "cersaiRegistrationNumber": "SI-2025-XXXXXX",
  "registrationDate": "2025-02-16",
  "filingDate": "2025-02-16",
  "filingDueDate": "2025-03-18",
  "acknowledgementUrl": "https://cersai.org.in/ack/SI-2025-XXXXXX.pdf"
}
```

---

## 4. GST Portal Integration

### Overview
Verify GST registration and returns filing status.

### API Details
| Item | Value |
|------|-------|
| Base URL | `https://api.gst.gov.in/commonapi/v1` |
| Auth | API Key + Session Token |
| Format | JSON |
| Timeout | 30 seconds |

### Endpoints

#### 4.1 Search Taxpayer
```
GET /search?gstin={gstin}
Authorization: Bearer {session_token}
```

**Response:**
```json
{
  "gstin": "27ABCDE1234F1Z5",
  "tradeName": "ABC Enterprises",
  "legalName": "ABC Enterprises Private Limited",
  "constitution": "Private Limited Company",
  "status": "Active",
  "registrationDate": "2017-07-01",
  "address": {
    "building": "Tower A",
    "street": "Commercial Complex",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001"
  },
  "natureOfBusiness": ["Retail Trade", "Wholesale Trade"],
  "lastReturnFiled": {
    "returnType": "GSTR-3B",
    "period": "JAN-2025",
    "filingDate": "2025-02-10"
  }
}
```

#### 4.2 Returns Filing Status
```
GET /returns/status?gstin={gstin}&fy={financial_year}
```

**Response:**
```json
{
  "gstin": "27ABCDE1234F1Z5",
  "financialYear": "2024-25",
  "returns": [
    {
      "returnType": "GSTR-3B",
      "period": "APR-2024",
      "filingDate": "2024-05-20",
      "status": "Filed"
    },
    {
      "returnType": "GSTR-1",
      "period": "APR-2024",
      "filingDate": "2024-05-11",
      "status": "Filed"
    }
  ],
  "complianceRating": "EXCELLENT",
  "defaultCount": 0
}
```

---

## 5. Income Tax (ITD) Integration

### Overview
PAN verification and ITR filing status.

### API Details
| Item | Value |
|------|-------|
| Base URL | `https://api.incometax.gov.in/v1` |
| Auth | mTLS + API Key |
| Format | JSON |
| Timeout | 30 seconds |

### Endpoints

#### 5.1 PAN Verification
```
POST /pan/verify
Content-Type: application/json
X-API-Key: {api_key}
```

**Request:**
```json
{
  "pan": "ABCDE1234F",
  "name": "JOHN DOE",
  "dateOfBirth": "1990-05-15",
  "consentToken": "{consent_artifact_id}"
}
```

**Response:**
```json
{
  "pan": "ABCDE1234F",
  "verified": true,
  "nameMatch": true,
  "nameOnRecord": "JOHN DOE",
  "status": "VALID",
  "panType": "INDIVIDUAL",
  "lastAssessedYear": "2024-25",
  "aadhaarLinked": true
}
```

#### 5.2 ITR Verification (Account Aggregator)
```
POST /itr/summary
Content-Type: application/json
```

**Request (with AA consent):**
```json
{
  "pan": "ABCDE1234F",
  "assessmentYears": ["2023-24", "2022-23"],
  "consentId": "{aa_consent_id}",
  "consentHandle": "{aa_consent_handle}"
}
```

**Response:**
```json
{
  "pan": "ABCDE1234F",
  "itrSummary": [
    {
      "assessmentYear": "2023-24",
      "itrType": "ITR-1",
      "filingDate": "2023-07-25",
      "grossTotalIncome": 1200000,
      "totalIncome": 1050000,
      "taxPayable": 105000,
      "taxPaid": 105000,
      "refundStatus": "NA"
    },
    {
      "assessmentYear": "2022-23",
      "itrType": "ITR-1",
      "filingDate": "2022-07-28",
      "grossTotalIncome": 1000000,
      "totalIncome": 850000,
      "taxPayable": 72500,
      "taxPaid": 72500
    }
  ]
}
```

---

## 6. Account Aggregator Integration

### Overview
Consent-based financial data sharing (RBI regulated).

### API Details
| Item | Value |
|------|-------|
| Base URL | `https://api.{aa-provider}.in/v2` |
| Auth | OAuth 2.0 + JWS |
| Format | JSON |
| Timeout | 30 seconds |

### Endpoints

#### 6.1 Consent Request
```
POST /Consent
Content-Type: application/json
Authorization: Bearer {access_token}
```

**Request:**
```json
{
  "ver": "2.0.0",
  "timestamp": "2025-02-16T10:30:00.000Z",
  "txnid": "{uuid}",
  "ConsentDetail": {
    "consentStart": "2025-02-16T10:30:00.000Z",
    "consentExpiry": "2025-02-23T10:30:00.000Z",
    "consentMode": "VIEW",
    "fetchType": "ONETIME",
    "consentTypes": ["PROFILE", "SUMMARY", "TRANSACTIONS"],
    "fiTypes": ["DEPOSIT", "RECURRING_DEPOSIT", "TERM_DEPOSIT"],
    "DataConsumer": {
      "id": "loanflow-fiu@finvu",
      "type": "FIU"
    },
    "Customer": {
      "id": "{customer_mobile}@finvu",
      "Identifiers": [
        {"type": "MOBILE", "value": "9876543210"},
        {"type": "PAN", "value": "ABCDE1234F"}
      ]
    },
    "Purpose": {
      "code": "101",
      "refUri": "https://api.rebit.org.in/aa/purpose/101.xml",
      "text": "Loan application processing"
    },
    "FIDataRange": {
      "from": "2024-02-01T00:00:00.000Z",
      "to": "2025-02-16T00:00:00.000Z"
    },
    "DataLife": {
      "unit": "DAY",
      "value": 30
    },
    "Frequency": {
      "unit": "MONTH",
      "value": 1
    }
  }
}
```

**Response:**
```json
{
  "ver": "2.0.0",
  "timestamp": "2025-02-16T10:30:05.000Z",
  "txnid": "{uuid}",
  "ConsentHandle": "{consent_handle}",
  "Customer": {
    "id": "9876543210@finvu"
  }
}
```

#### 6.2 Fetch Financial Information
```
POST /FI/fetch
Content-Type: application/json
```

**Request:**
```json
{
  "ver": "2.0.0",
  "timestamp": "2025-02-16T11:00:00.000Z",
  "txnid": "{uuid}",
  "consentId": "{consent_id}",
  "sessionId": "{session_id}",
  "KeyMaterial": {
    "cryptoAlg": "ECDH",
    "curve": "Curve25519",
    "params": "Ephemeral Key",
    "DHPublicKey": {
      "expiry": "2025-02-16T12:00:00.000Z",
      "Parameters": "",
      "KeyValue": "{base64_public_key}"
    },
    "Nonce": "{nonce}"
  }
}
```

**Response (Decrypted):**
```json
{
  "ver": "2.0.0",
  "timestamp": "2025-02-16T11:00:05.000Z",
  "FI": [
    {
      "fipId": "HDFC-FIP",
      "data": [
        {
          "linkRefNumber": "{ref}",
          "maskedAccNumber": "XXXX1234",
          "fiType": "DEPOSIT",
          "account": {
            "type": "SAVINGS",
            "branch": "Mumbai Main",
            "openingDate": "2020-01-15",
            "currentBalance": 125000,
            "currency": "INR",
            "Summary": {
              "currentBalance": 125000,
              "avgBalance6Months": 98000,
              "totalCredits6Months": 450000,
              "totalDebits6Months": 420000
            },
            "Transactions": [
              {
                "txnId": "TXN123",
                "type": "CREDIT",
                "mode": "SALARY",
                "amount": 75000,
                "narration": "SALARY-JAN-2025",
                "valueDate": "2025-01-28"
              }
            ]
          }
        }
      ]
    }
  ]
}
```

---

## 7. Penny Drop Verification

### Overview
Bank account verification via NPCI.

### API Details
| Item | Value |
|------|-------|
| Base URL | Via banking partner API |
| Auth | API Key |
| Format | JSON |
| Timeout | 60 seconds |

### Endpoint

```
POST /verify/bank-account
Content-Type: application/json
```

**Request:**
```json
{
  "accountNumber": "1234567890123456",
  "ifscCode": "HDFC0001234",
  "accountHolderName": "JOHN DOE",
  "verificationType": "PENNY_DROP"
}
```

**Response:**
```json
{
  "verificationId": "VER-2025-001234",
  "status": "SUCCESS",
  "accountExists": true,
  "nameMatch": true,
  "matchScore": 95,
  "beneficiaryName": "JOHN DOE",
  "bankName": "HDFC Bank",
  "branchName": "Mumbai Main Branch",
  "accountType": "SAVINGS",
  "utr": "HDFC25021600001234"
}
```

---

## 8. Integration Error Handling

### Retry Strategy
```yaml
retry_config:
  max_attempts: 3
  initial_delay_ms: 1000
  max_delay_ms: 10000
  exponential_backoff: true
  jitter: true
  retryable_errors:
    - CONNECTION_TIMEOUT
    - SERVICE_UNAVAILABLE
    - RATE_LIMITED
  non_retryable_errors:
    - INVALID_REQUEST
    - AUTHENTICATION_FAILED
    - NOT_FOUND
```

### Circuit Breaker
```yaml
circuit_breaker:
  failure_threshold: 5
  success_threshold: 3
  timeout_duration_sec: 60
  half_open_requests: 3
```

### Fallback Strategy
| Integration | Primary | Fallback |
|-------------|---------|----------|
| Credit Bureau | CIBIL | EXPERIAN → EQUIFAX |
| e-KYC | UIDAI | Offline KYC |
| Account Verification | Penny Drop | Account Aggregator |

---

## 9. Security Requirements

### mTLS Configuration
```yaml
mtls:
  client_cert: /etc/loanflow/certs/client.pem
  client_key: /etc/loanflow/certs/client.key
  ca_cert: /etc/loanflow/certs/ca-bundle.pem
  verify_hostname: true
  cipher_suites:
    - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
```

### Data Encryption
- **At Rest**: AES-256-GCM
- **In Transit**: TLS 1.3
- **PII Fields**: Field-level encryption with HSM-managed keys
- **Key Rotation**: Every 90 days

### Audit Logging
All integration calls must log:
- Request ID
- Timestamp
- API endpoint
- Request hash (no PII)
- Response status
- Latency
- Error details (if any)

---

## 10. Environment Configuration

```yaml
# application-prod.yml
integrations:
  cibil:
    base_url: https://api.cibil.com/v2
    member_code: ${CIBIL_MEMBER_CODE}
    timeout_ms: 30000

  uidai:
    base_url: https://auth.uidai.gov.in
    aua_code: ${UIDAI_AUA_CODE}
    sub_aua: ${UIDAI_SUB_AUA}
    license_key: ${UIDAI_LICENSE_KEY}

  cersai:
    base_url: https://api.cersai.org.in/api/v1
    client_id: ${CERSAI_CLIENT_ID}
    client_secret: ${CERSAI_CLIENT_SECRET}

  gst:
    base_url: https://api.gst.gov.in/commonapi/v1
    api_key: ${GST_API_KEY}

  account_aggregator:
    provider: finvu
    base_url: https://api.finvu.in/v2
    fiu_id: loanflow-fiu@finvu
```
