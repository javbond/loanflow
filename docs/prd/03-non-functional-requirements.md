# LoanFlow - Non-Functional Requirements

## NFR-001: Performance

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001.1 | Page load time | < 2 seconds |
| NFR-001.2 | Policy evaluation time | < 100 ms |
| NFR-001.3 | Credit bureau API response | < 5 seconds |
| NFR-001.4 | Concurrent users supported | 500+ |
| NFR-001.5 | Daily application volume | 10,000+ |

---

## NFR-002: Security

| ID | Requirement | Standard |
|----|-------------|----------|
| NFR-002.1 | Data encryption at rest | AES-256 |
| NFR-002.2 | Data encryption in transit | TLS 1.3 |
| NFR-002.3 | PII masking in logs | GDPR/DPDP compliant |
| NFR-002.4 | Authentication | OAuth 2.0 / OIDC |
| NFR-002.5 | Session management | JWT with refresh tokens |
| NFR-002.6 | API security | Rate limiting, API keys |
| NFR-002.7 | Vulnerability scanning | OWASP Top 10 compliance |
| NFR-002.8 | Penetration testing | Annual third-party audit |

---

## NFR-003: Availability

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-003.1 | System uptime | 99.9% |
| NFR-003.2 | RTO (Recovery Time Objective) | < 1 hour |
| NFR-003.3 | RPO (Recovery Point Objective) | < 15 minutes |
| NFR-003.4 | Planned maintenance window | Off-peak hours only |

---

## NFR-004: Scalability

| ID | Requirement | Approach |
|----|-------------|----------|
| NFR-004.1 | Horizontal scaling | Kubernetes auto-scaling |
| NFR-004.2 | Database scaling | Read replicas, partitioning |
| NFR-004.3 | Caching strategy | Redis for policies, sessions |

---

## NFR-005: Compliance & Audit

| ID | Requirement | Standard |
|----|-------------|----------|
| NFR-005.1 | Audit logging | All CRUD operations logged |
| NFR-005.2 | Data retention | 8 years (RBI mandate) |
| NFR-005.3 | Audit trail | Immutable, timestamped |
| NFR-005.4 | DPDP Act compliance | Consent management, data deletion |

---

## Security Architecture

```
                              PERIMETER SECURITY
  +-------------+  +-------------+  +-------------+  +-------------------------+
  | WAF         |  | DDoS        |  | Bot         |  | Geo-blocking            |
  | (AWS/Akamai)|  | Protection  |  | Detection   |  | (India only for prod)   |
  +-------------+  +-------------+  +-------------+  +-------------------------+
                                       |
                                       v
                              API GATEWAY SECURITY
  +-------------+  +-------------+  +-------------+  +-------------------------+
  | Rate        |  | API Key     |  | JWT         |  | mTLS                    |
  | Limiting    |  | Validation  |  | Validation  |  | (Service-to-Service)    |
  +-------------+  +-------------+  +-------------+  +-------------------------+
                                       |
                                       v
                              APPLICATION SECURITY
  +----------------------------------------------------------------------+
  | OWASP TOP 10 CONTROLS                                                 |
  +-----------------------------------------------------------------------+
  | A01 - Broken Access Control     -> RBAC + Attribute-based checks      |
  | A02 - Cryptographic Failures    -> AES-256, TLS 1.3, secure key store |
  | A03 - Injection                 -> Parameterized queries, validation  |
  | A04 - Insecure Design           -> Threat modeling, security reviews  |
  | A05 - Security Misconfiguration -> Hardened configs, secret mgmt      |
  | A06 - Vulnerable Components     -> Dependency scanning (Snyk)         |
  | A07 - Auth Failures             -> Keycloak, MFA, session management  |
  | A08 - Data Integrity Failures   -> Input validation, signed artifacts |
  | A09 - Logging Failures          -> Centralized logging, audit trails  |
  | A10 - SSRF                      -> Allowlist external calls           |
  +-----------------------------------------------------------------------+
                                       |
                                       v
                              DATA SECURITY
  +-------------------------+  +-------------------------+  +------------------+
  | Encryption at Rest      |  | Encryption in Transit   |  | Key Management   |
  | - AES-256 (DB)          |  | - TLS 1.3               |  | - AWS KMS / HSM  |
  | - AES-256 (Files)       |  | - mTLS (internal)       |  | - Key Rotation   |
  +-------------------------+  +-------------------------+  +------------------+

  +-------------------------+  +-------------------------+  +------------------+
  | PII Protection          |  | Data Masking            |  | Tokenization     |
  | - Aadhaar (masked)      |  | - Logs (auto-mask)      |  | - PAN tokenized  |
  | - Mobile (last 4)       |  | - UI (role-based)       |  | - Account numbers|
  +-------------------------+  +-------------------------+  +------------------+
```

---

## Secure Coding Standards

```yaml
# SECURE CODING STANDARDS (to be enforced via SonarQube)

SQL_INJECTION:
  - NEVER use string concatenation for SQL
  - ALWAYS use parameterized queries or JPA
  - Use @Query with named parameters only

XSS_PREVENTION:
  - Angular auto-escapes by default - DO NOT bypass
  - Sanitize user input on server side
  - Use Content-Security-Policy headers

AUTHENTICATION:
  - Use Keycloak for all authentication
  - Implement MFA for underwriters and admins
  - Session timeout: 15 minutes idle, 8 hours max
  - Password policy: 12+ chars, complexity, no reuse

AUTHORIZATION:
  - Use Spring Security @PreAuthorize
  - Implement method-level security
  - Verify resource ownership in service layer

SENSITIVE_DATA:
  - Aadhaar: Store only last 4 digits or tokenized
  - PAN: Mask middle characters in logs/UI
  - Never log passwords, tokens, or keys
  - Use @JsonIgnore for sensitive fields

CRYPTOGRAPHY:
  - Use Bcrypt (strength 12) for passwords
  - AES-256-GCM for symmetric encryption
  - RSA-2048 minimum for asymmetric
  - Never implement custom crypto

API_SECURITY:
  - Rate limit: 100 requests/minute per user
  - Request size limit: 10MB
  - Validate Content-Type headers
  - Implement request signing for critical operations

DEPENDENCY_MANAGEMENT:
  - Weekly Dependabot/Snyk scans
  - No vulnerable dependencies in production
  - Pin dependency versions
  - Use only approved libraries
```

---

## Regulatory Compliance Checklist

```
RBI GUIDELINES
+-- [ ] KYC Norms (Master Direction 2016)
|   +-- [ ] Customer Due Diligence (CDD)
|   +-- [ ] Enhanced Due Diligence for high-risk
|   +-- [ ] CKYC integration
|   +-- [ ] Periodic KYC refresh
+-- [ ] Fair Practices Code
|   +-- [ ] Loan application acknowledgment
|   +-- [ ] Rejection reason communication
|   +-- [ ] Sanction letter with all terms
|   +-- [ ] Prepayment/foreclosure disclosure
+-- [ ] Interest Rate Guidelines
|   +-- [ ] Reducing balance method
|   +-- [ ] APR disclosure
|   +-- [ ] Reset clause transparency
+-- [ ] Digital Lending Guidelines (Sep 2022)
    +-- [ ] Key Fact Statement (KFS)
    +-- [ ] Cooling-off period
    +-- [ ] Grievance redressal disclosure

DATA PROTECTION
+-- [ ] DPDP Act 2023 Compliance
|   +-- [ ] Consent management
|   +-- [ ] Data principal rights
|   +-- [ ] Data localization (India)
|   +-- [ ] Breach notification
+-- [ ] IT Act 2000 (Section 43A)
    +-- [ ] Reasonable security practices

INDUSTRY STANDARDS
+-- [ ] ISO 27001 alignment
+-- [ ] PCI-DSS (if card data)
+-- [ ] SOC 2 Type II readiness
```
