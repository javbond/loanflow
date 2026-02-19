# LoanFlow - Low-Level Design (LLD) - Tactical Design

## Loan Application Aggregate

```
                    LOAN APPLICATION AGGREGATE

                         +-------------------------+
                         |   LoanApplication       | <-- AGGREGATE ROOT
                         |   (Entity)              |
                         +-------------------------+
                         | - applicationId: UUID   |
                         | - applicationNumber: ARN|
                         | - loanType: LoanType    |
                         | - requestedAmount: Money|
                         | - term: LoanTerm        |
                         | - purpose: String       |
                         | - status: AppStatus     |
                         | - branch: BranchCode    |
                         +-----------+-------------+
                                     |
            +------------------------+------------------------+
            |                        |                        |
            v                        v                        v
+-------------------+    +-------------------+    +-------------------+
|   Applicant       |    |   Collateral      |    |   LoanOffer       |
|   (Entity)        |    |   (Entity)        |    |   (Entity)        |
+-------------------+    +-------------------+    +-------------------+
| - applicantId     |    | - collateralId    |    | - offerId         |
| - applicantType   |    | - type: CollType  |    | - amount: Money   |
| - pan: PAN        |    | - value: Money    |    | - interestRate    |
| - aadhaar: Aadhaar|    | - description     |    | - tenure          |
| - name: Name      |    | - location        |    | - emi: Money      |
| - dob: Date       |    | - legalStatus     |    | - processingFee   |
| - address: Address|    | - valuationDate   |    | - status          |
| - employment      |    | - valuationAmount |    | - validUntil      |
| - income: Money   |    +-------------------+    +-------------------+
+-------------------+
            |
            v
+-------------------+
|   Employment      |
|   (Value Object)  |
+-------------------+
| - type: EmpType   |
| - employer: String|
| - designation     |
| - since: Date     |
| - income: Money   |
+-------------------+

VALUE OBJECTS:
+---------+ +---------+ +---------+ +---------+ +---------+
|  Money  | |   PAN   | | Aadhaar | | Address | |  Name   |
+---------+ +---------+ +---------+ +---------+ +---------+
```

---

## Policy Aggregate

```
                       POLICY AGGREGATE

                         +-------------------------+
                         |      Policy             | <-- AGGREGATE ROOT
                         |      (Entity)           |
                         +-------------------------+
                         | - policyId: UUID        |
                         | - name: String          |
                         | - description: String   |
                         | - state: PolicyState    |
                         | - priority: int         |
                         | - effectiveFrom: Date   |
                         | - effectiveTo: Date     |
                         | - version: String       |
                         +-----------+-------------+
                                     |
                    +----------------+----------------+
                    |                |                |
                    v                v                v
        +-------------------+ +---------------+ +-------------------+
        | ConditionGroup    | | ActionSet     | | Outcome           |
        | (Entity)          | | (Entity)      | | (Value Object)    |
        +-------------------+ +---------------+ +-------------------+
        | - operator: AND/OR| | - operator    | | - onMatch: Action |
        | - conditions[]    | | - actions[]   | | - workflowId      |
        +-------------------+ +---------------+ +-------------------+
                |                    |
                v                    v
        +-------------------+ +-------------------+
        | Condition         | | Action            |
        | (Value Object)    | | (Value Object)    |
        +-------------------+ +-------------------+
        | - field: String   | | - type: ActionType|
        | - operator: Op    | | - parameters: Map |
        | - value: Any      | +-------------------+
        +-------------------+
```

---

## MongoDB Policy Document Schema

```json
{
  "_id": "ObjectId",
  "policyId": "UUID",
  "name": "Personal Loan Eligibility",
  "version": "1.0.0",
  "state": "ACTIVE",
  "priority": 100,
  "effectiveFrom": "ISODate",
  "effectiveTo": "ISODate",
  "conditionGroups": [
    {
      "operator": "AND",
      "conditions": [
        {"field": "applicant.age", "operator": "BETWEEN", "value": [21, 60]},
        {"field": "applicant.income", "operator": "GTE", "value": 25000}
      ]
    }
  ],
  "actions": [
    {"type": "SET_WORKFLOW", "value": "personal-loan-standard"},
    {"type": "SET_DOCUMENT_CHECKLIST", "value": ["PAN", "AADHAAR", "INCOME_PROOF"]}
  ]
}
```

---

## Domain Events

### Loan Application Context Events

```java
LoanApplicationCreated { applicationId, applicantId, loanType, amount, timestamp }
LoanApplicationSubmitted { applicationId, submittedBy, timestamp }
ApplicantVerified { applicationId, applicantId, verificationType, result }
CreditCheckCompleted { applicationId, creditScore, bureauName, timestamp }
DocumentUploaded { applicationId, documentId, documentType, timestamp }
DocumentVerified { applicationId, documentId, verificationResult, timestamp }
UnderwriterAssigned { applicationId, underwriterId, assignedAt }
LoanApproved { applicationId, approvedAmount, interestRate, approvedBy, timestamp }
LoanRejected { applicationId, rejectionReasons[], rejectedBy, timestamp }
LoanDisbursed { applicationId, disbursedAmount, accountNumber, timestamp }
```

### Policy Engine Context Events

```java
PolicyCreated { policyId, name, createdBy, timestamp }
PolicyActivated { policyId, activatedBy, effectiveFrom }
PolicyDeactivated { policyId, deactivatedBy, reason }
PolicyEvaluated { policyId, applicationId, result, matchedConditions[], timestamp }
```

### Workflow Context Events

```java
WorkflowStarted { instanceId, applicationId, workflowDefinitionId, timestamp }
TaskCreated { taskId, instanceId, taskType, assignedTo, dueDate }
TaskCompleted { taskId, completedBy, outcome, timestamp }
WorkflowCompleted { instanceId, applicationId, finalStatus, timestamp }
```

### Compliance Context Events

```java
KYCVerificationCompleted { applicationId, applicantId, status, verifiedAt }
CERSAIRegistrationCompleted { applicationId, collateralId, registrationNumber }
AuditLogCreated { entityType, entityId, action, performedBy, timestamp }
```

---

## Drools Rule Example

### Eligibility Rule

```drl
rule "Personal Loan - Age Eligibility"
    when
        $app : LoanApplication(loanType == LoanType.PERSONAL)
        $applicant : Applicant(age < 21 || age > 60)
    then
        $app.addRejectionReason("Applicant age must be between 21 and 60");
        $app.setEligibilityStatus(EligibilityStatus.REJECTED);
end
```

### Rule Categories

| Category | Examples |
|----------|----------|
| Eligibility | Age limits, income thresholds, existing loan checks |
| Credit Scoring | CIBIL score mapping, negative marker detection |
| Pricing | Interest rate calculation, processing fee tiers |
| Compliance | RBI limits, PSL classification |

---

## Integration Specifications

### CIBIL Integration

- **Endpoint:** Consumer Credit Report API
- **Auth:** API Key + Digital Signature
- **Request:** PAN, Name, DOB, Address
- **Response:** Credit Score, Enquiries, Accounts, DPD

### UIDAI e-KYC

- **Mode:** OTP-based e-KYC
- **Endpoint:** Authentication API
- **Request:** Aadhaar Number, OTP
- **Response:** Demographic + Photo

---

## Test Pyramid

```
                              TEST PYRAMID

                              /\
                             /  \
                            / E2E\                 <- 10% (Cypress)
                           /------\                   UI flows, critical paths
                          /        \
                         /Integration\             <- 20% (TestContainers)
                        /--------------\               API tests, DB tests
                       /                \
                      /   Unit Tests     \          <- 70% (JUnit, Jasmine)
                     /--------------------\            Business logic, services
                    /                      \
                   /--------------------------\

Coverage Targets:
+-- Unit Tests:        > 80%
+-- Integration Tests: > 60%
+-- E2E Tests:        Critical paths only
+-- Security Tests:   OWASP ZAP + SonarQube
```

---

## TDD Workflow

```
                              TDD CYCLE

    +-------------+
    |   RED       | <-- Write failing test first
    | (Write Test)|
    +------+------+
           |
           v
    +-------------+
    |   GREEN     | <-- Write minimum code to pass
    | (Make Pass) |
    +------+------+
           |
           v
    +-------------+
    |  REFACTOR   | <-- Improve code, keep tests green
    |             |
    +------+------+
           |
           +----------------------------------------------> (Repeat)

EXAMPLE TDD FLOW FOR US-004 (Loan Application):

1. Write test: testCreateLoanApplication_ShouldGenerateARN()
   -> Test fails (no implementation)

2. Implement: LoanApplicationService.create()
   -> Test passes

3. Refactor: Extract ARN generation to separate service
   -> All tests still pass

4. Write next test: testCreateLoanApplication_ShouldValidatePAN()
   -> Continue cycle
```

---

## Definition of Done (DoD) - Global

```
                         DEFINITION OF DONE

  CODE QUALITY
  +-- [ ] Code compiles without errors
  +-- [ ] All unit tests pass (>80% coverage)
  +-- [ ] SonarQube quality gate passed
  |   +-- Bugs: 0
  |   +-- Vulnerabilities: 0
  |   +-- Code Smells: < 10
  |   +-- Duplications: < 3%
  +-- [ ] Code review approved (2 reviewers)
  +-- [ ] No TODO/FIXME comments for current sprint

  SECURITY
  +-- [ ] No critical/high vulnerabilities (Snyk/Dependabot)
  +-- [ ] OWASP ZAP scan passed
  +-- [ ] Secrets scan passed (no hardcoded credentials)
  +-- [ ] Security review for sensitive features

  DOCUMENTATION
  +-- [ ] API documentation updated (OpenAPI)
  +-- [ ] README updated (if new setup steps)
  +-- [ ] Architecture diagram updated (if structural changes)

  DEPLOYMENT
  +-- [ ] CI pipeline green
  +-- [ ] Deployed to staging
  +-- [ ] Integration tests pass on staging
  +-- [ ] Performance benchmark met (if applicable)

  ACCEPTANCE
  +-- [ ] Demo to PO
  +-- [ ] Acceptance criteria verified
  +-- [ ] PO sign-off
```
