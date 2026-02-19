# MongoDB Collection Schemas

## LoanFlow - Loan Origination System

This document defines MongoDB collection schemas for storing dynamic policies, workflow definitions, rules, and cached external data.

---

## Database: `loanflow_db`

### Collections Overview

| Collection | Purpose | Index Strategy |
|------------|---------|----------------|
| `policies` | Dynamic policy definitions (Entra-style) | policyId, state, priority, effectiveFrom |
| `policy_versions` | Policy version history | policyId, version, createdAt |
| `workflow_definitions` | BPMN process definitions | key, version, deploymentId |
| `drools_rules` | Decision engine rule packages | packageName, version, status |
| `credit_reports_cache` | Cached credit bureau responses | applicantPan, bureauName, fetchedAt (TTL) |
| `decision_logs` | Rule evaluation audit logs | applicationId, evaluatedAt |
| `document_extractions` | OCR extraction results | documentId, extractionType |

---

## Collection: `policies`

Dynamic policy definitions for loan eligibility, routing, and processing rules.

```javascript
// Collection: policies
{
  "_id": ObjectId("..."),
  "policyId": "UUID",                    // Unique policy identifier
  "name": "Personal Loan - Salaried Eligibility",
  "code": "PL_SAL_ELIG_001",            // Human-readable code
  "description": "Eligibility criteria for salaried personal loan applicants",

  // Classification
  "category": "ELIGIBILITY",             // ELIGIBILITY | ROUTING | PRICING | DOCUMENT | COMPLIANCE
  "subCategory": "INCOME_CHECK",
  "loanProducts": ["PL", "VL"],          // Applicable loan product codes
  "applicantTypes": ["PRIMARY", "CO_APPLICANT"],

  // State Management
  "state": "ACTIVE",                     // DRAFT | PENDING_APPROVAL | ACTIVE | INACTIVE | ARCHIVED
  "priority": 100,                       // Lower number = higher priority
  "version": "1.0.0",

  // Effective Period
  "effectiveFrom": ISODate("2025-01-01T00:00:00Z"),
  "effectiveTo": ISODate("2025-12-31T23:59:59Z"),

  // Condition Groups (AND/OR logic)
  "conditionGroups": [
    {
      "groupId": "CG001",
      "operator": "AND",                 // AND | OR
      "conditions": [
        {
          "conditionId": "C001",
          "field": "applicant.employmentType",
          "fieldType": "STRING",
          "operator": "EQUALS",          // See operator list below
          "value": "SALARIED",
          "description": "Applicant must be salaried"
        },
        {
          "conditionId": "C002",
          "field": "applicant.age",
          "fieldType": "NUMBER",
          "operator": "BETWEEN",
          "value": [21, 58],             // Array for BETWEEN
          "description": "Age between 21 and 58"
        },
        {
          "conditionId": "C003",
          "field": "applicant.monthlyIncome",
          "fieldType": "CURRENCY",
          "operator": "GTE",
          "value": 25000,
          "description": "Minimum monthly income Rs. 25,000"
        }
      ]
    },
    {
      "groupId": "CG002",
      "operator": "OR",
      "conditions": [
        {
          "conditionId": "C004",
          "field": "applicant.creditScore",
          "fieldType": "NUMBER",
          "operator": "GTE",
          "value": 700,
          "description": "Credit score >= 700"
        },
        {
          "conditionId": "C005",
          "field": "applicant.hasExistingRelationship",
          "fieldType": "BOOLEAN",
          "operator": "EQUALS",
          "value": true,
          "description": "Existing customer with good track record"
        }
      ]
    }
  ],

  // Cross-group logic
  "groupLogic": "CG001 AND CG002",        // Expression combining groups

  // Actions when policy matches
  "actions": [
    {
      "actionId": "A001",
      "actionType": "SET_ELIGIBILITY",
      "parameters": {
        "status": "ELIGIBLE",
        "maxAmount": 1500000,
        "maxTenure": 60
      }
    },
    {
      "actionId": "A002",
      "actionType": "SET_WORKFLOW",
      "parameters": {
        "workflowKey": "personal-loan-standard",
        "version": "1.0"
      }
    },
    {
      "actionId": "A003",
      "actionType": "SET_DOCUMENT_CHECKLIST",
      "parameters": {
        "documents": ["PAN_CARD", "AADHAAR_CARD", "SALARY_SLIP", "BANK_STATEMENT"]
      }
    },
    {
      "actionId": "A004",
      "actionType": "TRIGGER_NOTIFICATION",
      "parameters": {
        "templateCode": "ELIGIBILITY_CONFIRMED"
      }
    }
  ],

  // Outcome configuration
  "onMatch": {
    "continueEvaluation": false,          // Stop after this policy matches
    "outcomeType": "PASS"                 // PASS | FAIL | REFER
  },
  "onNoMatch": {
    "continueEvaluation": true,
    "outcomeType": "CONTINUE"
  },

  // Metadata
  "tags": ["salaried", "personal-loan", "eligibility"],
  "notes": "Standard eligibility for salaried employees",

  // Audit
  "createdAt": ISODate("2025-01-15T10:30:00Z"),
  "createdBy": {
    "userId": "UUID",
    "name": "Admin User",
    "email": "admin@bank.com"
  },
  "updatedAt": ISODate("2025-01-20T14:15:00Z"),
  "updatedBy": {
    "userId": "UUID",
    "name": "Product Manager",
    "email": "pm@bank.com"
  },
  "approvedAt": ISODate("2025-01-21T09:00:00Z"),
  "approvedBy": {
    "userId": "UUID",
    "name": "Senior Manager",
    "email": "sm@bank.com"
  }
}

// Supported Operators
// STRING: EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, IN, NOT_IN, REGEX
// NUMBER: EQUALS, NOT_EQUALS, GT, GTE, LT, LTE, BETWEEN, IN, NOT_IN
// BOOLEAN: EQUALS, NOT_EQUALS
// DATE: EQUALS, BEFORE, AFTER, BETWEEN, WITHIN_DAYS
// ARRAY: CONTAINS, CONTAINS_ALL, CONTAINS_ANY, IS_EMPTY, SIZE_EQUALS

// Indexes
db.policies.createIndex({ "policyId": 1 }, { unique: true });
db.policies.createIndex({ "code": 1 }, { unique: true });
db.policies.createIndex({ "state": 1, "priority": 1 });
db.policies.createIndex({ "category": 1, "state": 1 });
db.policies.createIndex({ "loanProducts": 1 });
db.policies.createIndex({ "effectiveFrom": 1, "effectiveTo": 1 });
db.policies.createIndex({ "tags": 1 });
```

---

## Collection: `policy_versions`

Version history for policy changes (audit trail).

```javascript
// Collection: policy_versions
{
  "_id": ObjectId("..."),
  "policyId": "UUID",                    // Reference to policies collection
  "version": "1.0.0",
  "versionNumber": 1,                    // Auto-incremented

  // Snapshot of policy at this version
  "snapshot": {
    // Complete policy document at this version
    "name": "Personal Loan - Salaried Eligibility",
    "conditionGroups": [...],
    "actions": [...]
    // ... all fields from policies collection
  },

  // Change metadata
  "changeType": "CREATED",               // CREATED | UPDATED | ACTIVATED | DEACTIVATED | ARCHIVED
  "changeReason": "Initial policy creation",
  "changedFields": ["conditionGroups", "actions"],

  // Audit
  "createdAt": ISODate("2025-01-15T10:30:00Z"),
  "createdBy": {
    "userId": "UUID",
    "name": "Admin User",
    "email": "admin@bank.com"
  }
}

// Indexes
db.policy_versions.createIndex({ "policyId": 1, "versionNumber": -1 });
db.policy_versions.createIndex({ "policyId": 1, "createdAt": -1 });
```

---

## Collection: `workflow_definitions`

BPMN workflow process definitions.

```javascript
// Collection: workflow_definitions
{
  "_id": ObjectId("..."),
  "key": "loan-origination-process",      // Process definition key
  "name": "Loan Origination Process",
  "version": 1,
  "deploymentId": "deployment-123",

  // BPMN Definition
  "bpmnXml": "<?xml version=\"1.0\"?><bpmn:definitions>...</bpmn:definitions>",

  // Process metadata
  "description": "Main loan origination workflow from application to disbursement",
  "category": "LOAN_PROCESSING",

  // Applicable products
  "loanProducts": ["PL", "HL", "VL"],

  // Stages/Tasks summary
  "stages": [
    {
      "stageId": "application_intake",
      "name": "Application Intake",
      "taskType": "USER_TASK",
      "assigneeRole": "LOAN_OFFICER",
      "slaHours": 4
    },
    {
      "stageId": "document_verification",
      "name": "Document Verification",
      "taskType": "USER_TASK",
      "assigneeRole": "DOCUMENT_VERIFIER",
      "slaHours": 24
    },
    {
      "stageId": "credit_check",
      "name": "Credit Bureau Check",
      "taskType": "SERVICE_TASK",
      "serviceDelegate": "creditBureauDelegate",
      "slaHours": 1
    },
    {
      "stageId": "policy_evaluation",
      "name": "Policy Evaluation",
      "taskType": "SERVICE_TASK",
      "serviceDelegate": "policyEvaluationDelegate",
      "slaHours": 1
    },
    {
      "stageId": "underwriting",
      "name": "Underwriting Review",
      "taskType": "USER_TASK",
      "assigneeRole": "UNDERWRITER",
      "slaHours": 48
    },
    {
      "stageId": "approval",
      "name": "Approval Decision",
      "taskType": "USER_TASK",
      "assigneeRole": "APPROVER",
      "slaHours": 24
    },
    {
      "stageId": "disbursement",
      "name": "Disbursement",
      "taskType": "SERVICE_TASK",
      "serviceDelegate": "disbursementDelegate",
      "slaHours": 4
    }
  ],

  // SLA Configuration
  "totalSlaHours": 96,
  "escalationRules": [
    {
      "triggerAfterHours": 24,
      "action": "NOTIFY_MANAGER"
    },
    {
      "triggerAfterHours": 48,
      "action": "ESCALATE_TO_SENIOR"
    }
  ],

  // Status
  "status": "ACTIVE",                    // DRAFT | ACTIVE | SUSPENDED | DEPRECATED
  "deployedAt": ISODate("2025-01-01T00:00:00Z"),

  // Audit
  "createdAt": ISODate("2024-12-15T10:00:00Z"),
  "createdBy": {
    "userId": "UUID",
    "name": "Workflow Admin"
  },
  "updatedAt": ISODate("2025-01-01T00:00:00Z"),
  "updatedBy": {
    "userId": "UUID",
    "name": "Workflow Admin"
  }
}

// Indexes
db.workflow_definitions.createIndex({ "key": 1, "version": -1 });
db.workflow_definitions.createIndex({ "deploymentId": 1 });
db.workflow_definitions.createIndex({ "status": 1 });
db.workflow_definitions.createIndex({ "loanProducts": 1 });
```

---

## Collection: `drools_rules`

Decision engine rule packages.

```javascript
// Collection: drools_rules
{
  "_id": ObjectId("..."),
  "packageName": "com.loanflow.rules.eligibility",
  "ruleName": "PersonalLoanEligibility",
  "version": "1.0.0",

  // Rule Definition
  "drlContent": `
package com.loanflow.rules.eligibility;

import com.loanflow.model.LoanApplication;
import com.loanflow.model.Applicant;
import com.loanflow.model.EligibilityResult;

rule "PL - Age Check"
    salience 100
    when
        $app : LoanApplication(loanType == "PL")
        $applicant : Applicant(age < 21 || age > 60)
    then
        $app.addRejectionReason("Applicant age must be between 21 and 60");
        $app.setEligibilityStatus(EligibilityStatus.REJECTED);
end

rule "PL - Minimum Income"
    salience 90
    when
        $app : LoanApplication(loanType == "PL")
        $applicant : Applicant(monthlyIncome < 25000)
    then
        $app.addRejectionReason("Minimum monthly income requirement not met");
        $app.setEligibilityStatus(EligibilityStatus.REJECTED);
end
  `,

  // Metadata
  "category": "ELIGIBILITY",             // ELIGIBILITY | CREDIT_SCORING | PRICING | COMPLIANCE
  "loanProducts": ["PL"],
  "description": "Personal loan eligibility rules",

  // Execution
  "priority": 1,                         // Rule package execution order
  "enabled": true,

  // Compilation
  "compiledAt": ISODate("2025-01-15T10:00:00Z"),
  "compilationStatus": "SUCCESS",        // SUCCESS | FAILED
  "compilationErrors": [],

  // Dependencies
  "imports": [
    "com.loanflow.model.LoanApplication",
    "com.loanflow.model.Applicant",
    "com.loanflow.model.EligibilityResult"
  ],
  "globals": [],

  // Status
  "status": "ACTIVE",                    // DRAFT | ACTIVE | INACTIVE | DEPRECATED

  // Audit
  "createdAt": ISODate("2025-01-10T10:00:00Z"),
  "createdBy": {
    "userId": "UUID",
    "name": "Rules Admin"
  },
  "updatedAt": ISODate("2025-01-15T10:00:00Z"),
  "updatedBy": {
    "userId": "UUID",
    "name": "Rules Admin"
  }
}

// Indexes
db.drools_rules.createIndex({ "packageName": 1, "version": -1 });
db.drools_rules.createIndex({ "category": 1, "status": 1 });
db.drools_rules.createIndex({ "loanProducts": 1 });
```

---

## Collection: `credit_reports_cache`

Cached credit bureau responses with TTL.

```javascript
// Collection: credit_reports_cache
{
  "_id": ObjectId("..."),
  "cacheKey": "CIBIL_ABCDE1234F_2025-02",   // Bureau_PAN_Month

  // Applicant Reference
  "applicantPan": "ABCDE1234F",
  "applicantName": "John Doe",

  // Bureau Details
  "bureauName": "CIBIL",                   // CIBIL | EXPERIAN | EQUIFAX | CRIF
  "inquiryType": "SOFT",                   // SOFT | HARD

  // Score Summary
  "creditScore": 750,
  "scoreVersion": "TransUnion CIBIL Score 2.0",
  "scoreBand": "EXCELLENT",                // EXCELLENT | GOOD | FAIR | POOR

  // Account Summary
  "summary": {
    "totalAccounts": 8,
    "activeAccounts": 5,
    "closedAccounts": 3,
    "overdueAccounts": 0,
    "writtenOffAccounts": 0,
    "totalOutstanding": 450000,
    "totalOverdue": 0,
    "oldestAccountAge": 96,                // Months
    "newestAccountAge": 12
  },

  // DPD (Days Past Due) History
  "dpdSummary": {
    "dpd30Count": 0,
    "dpd60Count": 0,
    "dpd90Count": 0,
    "maxDpd12Months": 0,
    "maxDpd24Months": 0
  },

  // Enquiry Summary
  "enquirySummary": {
    "last30Days": 1,
    "last90Days": 2,
    "last180Days": 4,
    "totalEnquiries": 15
  },

  // Full Report (compressed/encrypted)
  "fullReport": {
    "accounts": [
      {
        "accountType": "Credit Card",
        "lenderName": "HDFC Bank",
        "accountNumber": "XXXX1234",
        "sanctionAmount": 200000,
        "currentBalance": 45000,
        "dpd": 0,
        "accountStatus": "ACTIVE"
      }
      // ... more accounts
    ],
    "enquiries": [
      {
        "enquiryDate": ISODate("2025-01-10"),
        "lenderName": "ICICI Bank",
        "purpose": "Personal Loan",
        "amount": 500000
      }
    ]
  },

  // Request/Response tracking
  "requestReference": "REQ-123456",
  "responseReference": "RES-789012",

  // Cache metadata
  "fetchedAt": ISODate("2025-02-01T10:30:00Z"),
  "expiresAt": ISODate("2025-02-08T10:30:00Z"),    // TTL: 7 days
  "source": "LIVE_API",                    // LIVE_API | MOCK | CACHED

  // Status
  "status": "SUCCESS",                     // SUCCESS | FAILED | TIMEOUT | PARTIAL
  "errorMessage": null
}

// TTL Index - Auto-delete after expiry
db.credit_reports_cache.createIndex(
  { "expiresAt": 1 },
  { expireAfterSeconds: 0 }
);

// Other indexes
db.credit_reports_cache.createIndex({ "applicantPan": 1, "bureauName": 1 });
db.credit_reports_cache.createIndex({ "cacheKey": 1 }, { unique: true });
db.credit_reports_cache.createIndex({ "fetchedAt": -1 });
```

---

## Collection: `decision_logs`

Audit logs for policy and rule evaluations.

```javascript
// Collection: decision_logs
{
  "_id": ObjectId("..."),
  "logId": "UUID",

  // Application Reference
  "applicationId": "UUID",
  "applicationNumber": "LF20250201000123",

  // Evaluation Context
  "evaluationType": "POLICY",              // POLICY | DROOLS | COMBINED
  "evaluationPhase": "ELIGIBILITY",        // ELIGIBILITY | CREDIT | PRICING | APPROVAL

  // Input Data (snapshot)
  "inputData": {
    "applicant": {
      "age": 35,
      "employmentType": "SALARIED",
      "monthlyIncome": 75000,
      "creditScore": 750
    },
    "loan": {
      "product": "PL",
      "requestedAmount": 500000,
      "tenure": 36
    }
  },

  // Policies Evaluated
  "policiesEvaluated": [
    {
      "policyId": "UUID",
      "policyCode": "PL_SAL_ELIG_001",
      "policyName": "Personal Loan - Salaried Eligibility",
      "matched": true,
      "conditionsEvaluated": [
        {
          "conditionId": "C001",
          "field": "applicant.employmentType",
          "operator": "EQUALS",
          "expectedValue": "SALARIED",
          "actualValue": "SALARIED",
          "result": true
        },
        {
          "conditionId": "C002",
          "field": "applicant.age",
          "operator": "BETWEEN",
          "expectedValue": [21, 58],
          "actualValue": 35,
          "result": true
        }
      ],
      "actionsExecuted": [
        {
          "actionId": "A001",
          "actionType": "SET_ELIGIBILITY",
          "parameters": { "status": "ELIGIBLE" },
          "executed": true
        }
      ],
      "executionTimeMs": 15
    }
  ],

  // Rules Evaluated (Drools)
  "rulesEvaluated": [
    {
      "ruleName": "PL - Age Check",
      "packageName": "com.loanflow.rules.eligibility",
      "fired": false,
      "executionTimeMs": 2
    },
    {
      "ruleName": "PL - Minimum Income",
      "packageName": "com.loanflow.rules.eligibility",
      "fired": false,
      "executionTimeMs": 1
    }
  ],

  // Final Outcome
  "outcome": {
    "decision": "ELIGIBLE",                // ELIGIBLE | INELIGIBLE | REFER
    "reasons": [],
    "recommendedActions": ["PROCEED_TO_UNDERWRITING"],
    "parameters": {
      "maxEligibleAmount": 1500000,
      "recommendedTenure": 48,
      "workflowKey": "personal-loan-standard"
    }
  },

  // Performance
  "totalExecutionTimeMs": 45,
  "policiesCount": 5,
  "rulesCount": 12,

  // Audit
  "evaluatedAt": ISODate("2025-02-01T10:30:00Z"),
  "evaluatedBy": {
    "type": "SYSTEM",                      // SYSTEM | USER
    "userId": null,
    "serviceName": "policy-engine"
  }
}

// Indexes
db.decision_logs.createIndex({ "applicationId": 1 });
db.decision_logs.createIndex({ "applicationNumber": 1 });
db.decision_logs.createIndex({ "evaluatedAt": -1 });
db.decision_logs.createIndex({ "evaluationType": 1, "evaluationPhase": 1 });

// TTL Index - Keep for 2 years
db.decision_logs.createIndex(
  { "evaluatedAt": 1 },
  { expireAfterSeconds: 63072000 }  // 730 days
);
```

---

## Collection: `document_extractions`

OCR and data extraction results from uploaded documents.

```javascript
// Collection: document_extractions
{
  "_id": ObjectId("..."),
  "extractionId": "UUID",

  // Document Reference
  "documentId": "UUID",                    // Reference to PostgreSQL document
  "applicationId": "UUID",
  "applicantId": "UUID",

  // Document Type
  "documentType": "PAN_CARD",
  "extractionType": "OCR",                 // OCR | BARCODE | QR | NFC

  // Extraction Status
  "status": "COMPLETED",                   // PENDING | PROCESSING | COMPLETED | FAILED | MANUAL_REVIEW

  // Extracted Data
  "extractedData": {
    "panNumber": "ABCDE1234F",
    "name": "JOHN DOE",
    "fatherName": "RICHARD DOE",
    "dateOfBirth": "1990-05-15",
    "rawText": "INCOME TAX DEPARTMENT\nGOVT OF INDIA\n..."
  },

  // Confidence Scores
  "confidence": {
    "overall": 0.95,
    "fields": {
      "panNumber": 0.99,
      "name": 0.97,
      "fatherName": 0.92,
      "dateOfBirth": 0.98
    }
  },

  // Validation Results
  "validations": [
    {
      "field": "panNumber",
      "validationType": "FORMAT",
      "passed": true,
      "message": "Valid PAN format"
    },
    {
      "field": "panNumber",
      "validationType": "CHECKSUM",
      "passed": true,
      "message": "PAN checksum verified"
    },
    {
      "field": "name",
      "validationType": "MATCH",
      "passed": true,
      "matchedWith": "applicantName",
      "matchScore": 0.95
    }
  ],

  // OCR Engine Details
  "engineUsed": "TESSERACT",              // TESSERACT | GOOGLE_VISION | AWS_TEXTRACT | AZURE_FORM
  "engineVersion": "5.3.0",

  // Image Quality
  "imageQuality": {
    "resolution": "1200x800",
    "dpi": 300,
    "brightness": 0.8,
    "contrast": 0.7,
    "blur": 0.1,
    "qualityScore": 0.85
  },

  // Processing Time
  "processingTimeMs": 1250,

  // Manual Review
  "requiresManualReview": false,
  "manualReviewReason": null,
  "manuallyVerified": false,
  "manuallyVerifiedBy": null,
  "manuallyVerifiedAt": null,

  // Audit
  "extractedAt": ISODate("2025-02-01T10:35:00Z"),
  "updatedAt": ISODate("2025-02-01T10:35:00Z")
}

// Indexes
db.document_extractions.createIndex({ "documentId": 1 });
db.document_extractions.createIndex({ "applicationId": 1 });
db.document_extractions.createIndex({ "status": 1 });
db.document_extractions.createIndex({ "documentType": 1 });
db.document_extractions.createIndex({ "extractedAt": -1 });
```

---

## Data Validation Schema (JSON Schema)

### Policy Validation Schema

```javascript
// Used by Mongoose or MongoDB validator
const policySchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["policyId", "name", "code", "category", "state", "conditionGroups", "actions"],
    properties: {
      policyId: {
        bsonType: "string",
        pattern: "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
      },
      name: {
        bsonType: "string",
        minLength: 3,
        maxLength: 200
      },
      code: {
        bsonType: "string",
        pattern: "^[A-Z0-9_]+$",
        maxLength: 50
      },
      category: {
        enum: ["ELIGIBILITY", "ROUTING", "PRICING", "DOCUMENT", "COMPLIANCE"]
      },
      state: {
        enum: ["DRAFT", "PENDING_APPROVAL", "ACTIVE", "INACTIVE", "ARCHIVED"]
      },
      priority: {
        bsonType: "int",
        minimum: 1,
        maximum: 9999
      },
      conditionGroups: {
        bsonType: "array",
        minItems: 1,
        items: {
          bsonType: "object",
          required: ["groupId", "operator", "conditions"],
          properties: {
            operator: { enum: ["AND", "OR"] },
            conditions: {
              bsonType: "array",
              minItems: 1
            }
          }
        }
      },
      actions: {
        bsonType: "array",
        minItems: 1
      }
    }
  }
};

db.createCollection("policies", { validator: policySchema });
```

---

## Migration Scripts

### Initialize Collections

```javascript
// Initialize LoanFlow MongoDB Collections

// 1. Create collections with validation
db.createCollection("policies");
db.createCollection("policy_versions");
db.createCollection("workflow_definitions");
db.createCollection("drools_rules");
db.createCollection("credit_reports_cache");
db.createCollection("decision_logs");
db.createCollection("document_extractions");

// 2. Create indexes (as defined above for each collection)

// 3. Insert seed data for testing

// Sample eligibility policy
db.policies.insertOne({
  policyId: "550e8400-e29b-41d4-a716-446655440001",
  name: "Personal Loan - Basic Eligibility",
  code: "PL_BASIC_ELIG",
  description: "Basic eligibility check for personal loans",
  category: "ELIGIBILITY",
  loanProducts: ["PL"],
  state: "ACTIVE",
  priority: 100,
  version: "1.0.0",
  effectiveFrom: new Date("2025-01-01"),
  effectiveTo: new Date("2025-12-31"),
  conditionGroups: [
    {
      groupId: "CG001",
      operator: "AND",
      conditions: [
        {
          conditionId: "C001",
          field: "applicant.age",
          fieldType: "NUMBER",
          operator: "BETWEEN",
          value: [21, 60],
          description: "Age between 21 and 60"
        },
        {
          conditionId: "C002",
          field: "applicant.monthlyIncome",
          fieldType: "CURRENCY",
          operator: "GTE",
          value: 25000,
          description: "Minimum monthly income Rs. 25,000"
        }
      ]
    }
  ],
  actions: [
    {
      actionId: "A001",
      actionType: "SET_ELIGIBILITY",
      parameters: {
        status: "ELIGIBLE"
      }
    }
  ],
  onMatch: {
    continueEvaluation: false,
    outcomeType: "PASS"
  },
  tags: ["personal-loan", "basic", "eligibility"],
  createdAt: new Date(),
  createdBy: {
    userId: "system",
    name: "System",
    email: "system@loanflow.com"
  }
});

print("MongoDB collections initialized successfully!");
```

---

## Usage Notes

1. **Policies Collection**: Core of the dynamic policy engine. Evaluated in priority order.

2. **Credit Reports Cache**: Uses TTL index for automatic cleanup. Cache duration: 7 days.

3. **Decision Logs**: Immutable audit trail. Retained for 2 years per compliance requirements.

4. **Document Extractions**: Stores OCR results separately from document metadata for flexibility.

5. **Connection String Example**:
   ```
   mongodb://loanflow_user:password@localhost:27017/loanflow_db?authSource=admin&replicaSet=rs0
   ```

6. **Recommended Settings**:
   - Enable journaling for durability
   - Use replica set for high availability
   - Enable encryption at rest
   - Configure read preference based on use case
