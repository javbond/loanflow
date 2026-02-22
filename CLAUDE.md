# LoanFlow Project - Claude Code Instructions

## Project Overview
**LoanFlow** - Loan Origination System for Indian Banks
- Multi-tenant SaaS platform
- Spring Boot 3.2 microservices + Angular 17 frontend
- TDD approach with 344+ tests

---

## 📚 WORKFLOW DOCUMENTATION

| Document | Location | Purpose |
|----------|----------|---------|
| **Full SDLC Workflow** | `docs/workflow/SDLC.md` | Complete development process |
| **Sprint Plans** | `docs/sprints/sprint-N-plan.md` | Current sprint details |
| **Sprint Reviews** | `docs/sprints/sprint-N-review.md` | Sprint outcomes |
| **Product Backlog** | `docs/prd/06-backlog-epics.md` | All epics & stories |
| **Roadmap** | `docs/prd/07-milestones-roadmap.md` | Milestones timeline |

### GitHub Integration
- **Project Board**: [LoanFlow Development](https://github.com/users/javbond/projects/2)
- **Milestones**: GitHub → Milestones tab
- **Issues**: All epics/stories/tasks tracked as issues

---

## ⚠️ CRITICAL: SPRINT APPROVAL AUTOMATION

**When user approves a sprint plan, Claude MUST automatically:**

1. ✅ **Create GitHub Milestone** for the sprint
2. ✅ **Create GitHub Issues** for each Epic (if new) and User Story
3. ✅ **Link all issues** to the milestone
4. ✅ **Add issues to Project Board**
5. ✅ **Save sprint plan** to `docs/sprints/sprint-N-plan.md`
6. ✅ **Update this file** with current sprint status
7. ✅ **Confirm with URLs** of all created artifacts

**This is NOT optional - all items must be created on approval.**

---

## 🚨 AUTOMATIC TRIGGERS

### User Says "Sprint Planning" / "Start Sprint":
→ Run `/scrum-sprint planning` workflow
→ Propose sprint, on approval create all GitHub artifacts

### User Says "Sprint Progress" / "Daily Standup":
→ Run `/scrum-sprint progress` workflow
→ Check GitHub issues, show status report

### User Says "Sprint Review" / "End Sprint":
→ Run `/scrum-sprint review` workflow
→ Close milestone, generate review document

### User Says "Add to Backlog" / "New Feature":
→ Run `/agile-backlog` workflow
→ Create GitHub issue, update backlog doc

### User Completes a Feature:
→ Update GitHub issue with comment
→ Close the issue
→ Update sprint progress below

---

## 📊 CURRENT SPRINT STATUS

### Sprint 8 (CIBIL + Income Verification + Document Upload) - 🔄 IN PROGRESS
**Milestone**: [Sprint 8](https://github.com/javbond/loanflow/milestone/11)
**Duration**: 2026-04-03 to 2026-04-17
**Sprint Goal**: Integrate CIBIL credit bureau, add income verification pipeline (ITR/GST/bank statement), enhance document upload with virus scanning — Milestone 3 progress

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #44 | [US-016] Credit Bureau Integration (CIBIL API) | 8 | ✅ Complete |
| #45 | [US-017] Income Verification (ITR, GST, Bank Stmt) | 5 | ✅ Complete |
| #46 | [US-020] Document Upload Enhanced (Virus Scan) | 3 | ⏳ Pending |

**Total Story Points**: 16 | **Completed**: 13

**Progress:** `████████░░` 81%

---

### Sprint 7 (Drools + Approval Hierarchy + Risk Dashboard) - ✅ COMPLETED
**Milestone**: [Sprint 7](https://github.com/javbond/loanflow/milestone/10)
**Duration**: 2026-03-20 to 2026-04-03
**Sprint Goal**: Integrate PRD-mandated Drools engine, add amount-based approval routing, deliver risk analytics dashboard

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #40 | [US-018] Decision Engine (Drools) | 8 | ✅ Complete |
| #41 | [US-015] Approval Hierarchy | 5 | ✅ Complete |
| #42 | [US-019] Risk Dashboard | 3 | ✅ Complete |
| #43 | BUG: Task Inbox (UUID sync, claim, interest rate) | - | ✅ Fixed |

**Total Story Points**: 16 | **Completed**: 16
**Velocity**: 16 pts/sprint (3rd consecutive)
**Review**: `docs/sprints/sprint-7-review.md`

**Progress:** `██████████` 100%

---

### Sprint 6 (Policy Evaluation + Task Assignment) - ✅ COMPLETED
**Milestone**: [Sprint 6](https://github.com/javbond/loanflow/milestone/9)
**Duration**: 2026-03-06 to 2026-03-20
**Sprint Goal**: Complete M2 milestone (Policy Evaluation Engine) + Production-grade workflow assignment

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #37 | [US-010] Policy Evaluation Engine | 8 | ✅ Complete |
| #38 | [US-011] Pre-built Policy Templates | 3 | ✅ Complete |
| #39 | [US-014] Task Assignment & Escalation | 5 | ✅ Complete |

**Total Story Points**: 16 | **Completed**: 16
**Velocity**: 16 pts/sprint
**Review**: `docs/sprints/sprint-6-review.md`

**Progress:** `██████████` 100%

---

### Sprint 5 (Policy Engine Foundation) - ✅ COMPLETED
**Milestone**: [Sprint 5](https://github.com/javbond/loanflow/milestone/8)
**Duration**: 2026-02-20 to 2026-03-06
**Sprint Goal**: Policy Data Model, CRUD APIs, and Flowable BPMN workflow integration

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #12 | [EPIC-003] Dynamic Policy Engine | - | 🔄 Parent |
| #32 | [US-008] Policy Data Model & CRUD | 8 | ✅ Complete (51 tests) |
| #33 | [US-012] Flowable BPMN Integration | 5 | ✅ Complete (48 tests) |
| #35 | [US-013] Underwriter Workbench UI | - | ✅ Complete (18 files, 2457 lines) |

**Total Story Points**: 13 | **Completed**: 13
**PRs Merged**: #34 (US-008, US-012), #36 (US-013)

**Progress:** `██████████` 100%

---

### Sprint 4 (Customer Portal) - ✅ COMPLETED
**Milestone**: [Sprint 4](https://github.com/javbond/loanflow/milestone/7)
**Velocity**: 5 stories delivered

| Issue | Title | Status |
|-------|-------|--------|
| #22 | [EPIC-004] Customer Self-Service Portal | ✅ Closed |
| #26 | [US-024] Customer Loan Application Form | ✅ Closed |
| #27 | [US-025] Customer Document Upload | ✅ Closed |
| #28 | [US-026] Loan Offer Accept/Reject | ✅ Closed |
| #29 | [US-027] Document Download | ✅ Closed |
| #30 | [US-028] Customer Profile Management | ✅ Closed |
| #21 | [US-007] Application Status Tracking | ✅ Closed |
| #23 | [US-021] Customer Dashboard | ✅ Closed |
| #24 | [US-022] Customer Documents | ✅ Closed |
| #25 | [US-023] Application Status Timeline | ✅ Closed |

**PR Merged**: #31 (Customer Portal)
**Progress:** `██████████` 100%

---

### Sprint 3 (Security & Auth) - ✅ COMPLETED
**Milestone**: [Sprint 3](https://github.com/javbond/loanflow/milestone/6)
**Velocity**: 8 story points

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #3 | [US-002] Authentication System (Keycloak OAuth2/OIDC) | 8 | ✅ Closed |
| #4 | [US-003] Role-Based Access Control | 5 | 🔄 Partial (admin UI deferred) |

**PR Merged**: #20 (Auth System)
**Progress:** `██████████` 100%

---

### Sprint 2 (Loan & Document Management) - ✅ COMPLETED
**Milestone**: [Sprint 2](https://github.com/javbond/loanflow/milestone/5)
**Duration**: 2026-02-19 to 2026-03-05
**Velocity**: 13 story points

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #11 | [EPIC-002] Loan Application Management | - | ✅ Closed |
| #14 | [US-005] Loan Application Angular UI | 8 | ✅ Closed |
| #15 | [US-006] Document Management Angular UI | 5 | ✅ Closed |

**Bug Fixes (Sprint 2):**
| Issue | Description | Status |
|-------|-------------|--------|
| #16 | Document Upload UUID mismatch | ✅ Closed |
| #17 | MinIO credentials mismatch | ✅ Closed |
| #18 | Document verification verifierId | ✅ Closed |
| #19 | Documents list not visible | ✅ Closed |

**Progress:** `██████████` 100%

### Sprint 1 (Foundation + Customer) - ✅ COMPLETED
| Issue | Title | Status |
|-------|-------|--------|
| #2 | [US-001] Project Setup | ✅ Closed |
| #6 | Customer Backend API | ✅ Closed |
| #7 | Customer Angular UI | ✅ Closed |
| #13 | [US-004] Customer Management E2E | ✅ Closed |

**Velocity**: 16 story points

---

## 📋 BACKLOG (Future Sprints)

| Issue | Title | Priority | Sprint |
|-------|-------|----------|--------|
| US-016 | Credit Bureau Integration (CIBIL) | P1 | Sprint 8 |
| US-017 | Income Verification (ITR, GST, bank stmt) | P1 | Sprint 8 |
| US-020 | Document Upload (enhanced with virus scan) | P2 | Sprint 8-9 |
| US-024 | e-KYC Integration (UIDAI) | P2 | Sprint 9 |
| #4 | [US-003] RBAC Admin UI (remaining) | P2 | Deferred |

---

## 🔧 AVAILABLE COMMANDS

| Command | Phase | Description |
|---------|-------|-------------|
| `/scrum-sprint planning` | Sprint | Plan new sprint |
| `/scrum-sprint progress` | Sprint | Daily status |
| `/scrum-sprint review` | Sprint | End of sprint |
| `/scrum-sprint retro` | Sprint | Retrospective |
| `/scrum-sprint backlog` | Backlog | View/prioritize |
| `/agile-backlog` | Backlog | Create new items |

---

## 🏗️ SERVICES STATUS

| Service | Port | Tests | Backend | Frontend |
|---------|------|-------|---------|----------|
| customer-service | 8082 | 45 | ✅ Done | ✅ Done |
| loan-service | 8081 | 206 | ✅ Done (+ Flowable BPMN + Drools + Approval Hierarchy + CIBIL Bureau + Income Verification) | ✅ Done (+ Task Inbox + Risk Dashboard + Bureau Report + Income Panel) |
| document-service | 8083 | 49 | ✅ Done | ✅ Done |
| auth-service (Keycloak) | 8085 | 10 | ✅ Keycloak OAuth2/OIDC | ✅ Done (Login/Logout/Guards) |
| policy-service | 8086 | 66 | ✅ Done (MongoDB + Redis + Evaluation Engine) | ✅ Done (Policy Builder UI) |
| notification-service | 8084 | - | ⏳ Pending | ⏳ Pending |
| api-gateway | 8080 | - | ⏳ Pending | - |

**Total TDD Tests**: 344+ (auth-service: 10, customer: 45, loan: 206, document: 49, policy: 66, common: 8)

---

## 🚀 QUICK START

### ⚠️ CRITICAL: Java Environment Setup
**ALWAYS run this before any Maven command:**
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "$HOME/.sdkman/candidates/java/current")
```

**Why?** The build uses Java 20 syntax but often Java 17 is available. Without JAVA_HOME set, Lombok/MapStruct annotation processing fails with:
```
java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

### Start UAT Environment
```bash
# 1. ALWAYS set JAVA_HOME first!
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "$HOME/.sdkman/candidates/java/current")

# 2. Infrastructure
cd infrastructure && docker-compose up -d

# 3. Backend (UAT mode - security disabled)
cd backend/customer-service && mvn spring-boot:run -Dspring-boot.run.profiles=uat &
cd backend/loan-service && mvn spring-boot:run -Dspring-boot.run.profiles=uat &
cd backend/document-service && mvn spring-boot:run -Dspring-boot.run.profiles=uat &
cd backend/auth-service && mvn spring-boot:run -Dspring-boot.run.profiles=uat &

# 4. Frontend
cd frontend/loanflow-web && ng serve
```

### GitHub Commands
```bash
# Sprint status
gh issue list --milestone "Sprint 2" --state all

# Update progress
gh issue comment 14 --body "🔄 Progress: List component done"

# Close issue
gh issue close 14 --comment "✅ Completed: Loan UI implemented"
```

---

## ✅ DEFINITION OF DONE

- [ ] Code complete with unit tests (>80% coverage)
- [ ] Manual UAT testing passed
- [ ] GitHub issue closed with comment
- [ ] Sprint plan updated
- [ ] This file (CLAUDE.md) updated

---

## 🐛 BUG HANDLING PROTOCOL

**MANDATORY for all bugs discovered during development/UAT:**

1. **ALWAYS create GitHub Issue FIRST** before fixing any bug
   ```bash
   gh issue create --title "[BUG] Short description" --body "..." --label "bug" --milestone "Sprint N"
   ```

2. **Issue Labels**: `bug`, `P0/P1/P2` (priority), `sprint-N`, `frontend/backend`

3. **Reference issue in commit**:
   ```bash
   git commit -m "fix(component): description

   Fixes #XX"
   ```

4. **NEVER add bug details to plan file** - use GitHub Issues only

5. **Close issue after fix verified**:
   ```bash
   gh issue close XX --comment "✅ Fixed and verified"
   ```

---

## 📋 PLAN FILE OPTIMIZATION

**Keep plan file minimal to optimize tokens:**

1. **Plan file = CURRENT task only** (not cumulative history)
2. **Clear after task completion** - history lives in:
   - GitHub Issues (bugs, features)
   - `docs/sprints/` (sprint plans)
   - This file (project context)
3. **Max length**: ~50 lines per task
4. **On approval**: Only current task context loaded

---

## 🌿 GIT WORKFLOW PROTOCOL

**Branch Strategy: Feature Branch per Epic/User Story/Bug**

### Branch Naming Convention
```
feature/US-XXX-short-description   # User Stories
bugfix/BUG-XXX-short-description   # Bug fixes
epic/EPIC-XXX-short-description    # Epic-level branches (optional)
```

### Workflow Steps (MANDATORY)

1. **START of Epic/Story/Bug** → Create feature branch:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/US-006-document-management-ui
   ```

2. **DURING development** → Commit frequently with conventional commits:
   ```bash
   git commit -m "feat(document): add upload component

   Implements #15"
   ```

3. **END of Epic/Story/Bug** → After UAT approval, merge to main:
   ```bash
   # Ensure all tests pass
   npm run build && mvn test

   # Create PR or merge directly (after user approval)
   git checkout main
   git merge feature/US-006-document-management-ui
   git push origin main

   # Delete feature branch
   git branch -d feature/US-006-document-management-ui
   ```

### Commit Message Convention
```
type(scope): subject

body (optional)

Refs/Fixes #issue-number
```

**Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
**Scope**: `customer`, `loan`, `document`, `common`, etc.

### Merge Rules

| Action | Requires |
|--------|----------|
| Create feature branch | Auto on task start |
| Merge to main | User approval + all tests pass |
| Force push | NEVER (ask user first) |
| Direct commit to main | NEVER (except docs) |

### Branch Cleanup
After merge approval:
```bash
git branch -d feature/US-XXX
git push origin --delete feature/US-XXX  # if remote exists
```

---

## 🚨 MANDATORY PRD COMPLIANCE CHECKLIST

**BEFORE implementing ANY feature, Claude MUST:**

### Step 1: Read PRD Requirements
```bash
# Check backlog for task specifications
cat docs/prd/06-backlog-epics.md | grep -A 10 "US-XXX"

# Check milestones for acceptance criteria
cat docs/prd/07-milestones-roadmap.md

# Check HLD for architecture decisions
cat docs/hld/architecture.md  # if exists
```

### Step 2: Verify Tech Stack Compliance
**PRD-specified technologies - MUST USE:**
| Component | Required Technology | NOT Acceptable |
|-----------|---------------------|----------------|
| **Authentication** | Keycloak OAuth2/OIDC | Standalone JWT |
| **Workflow Engine** | Flowable BPMN | Custom state machine |
| **Decision Engine** | Drools DRL | Hardcoded rules |
| **Message Queue** | RabbitMQ | Direct HTTP calls |
| **Object Storage** | MinIO | Local file system |
| **Cache** | Redis | In-memory maps |
| **Search** | Elasticsearch | SQL LIKE queries |

### Step 3: Cross-Check Before Coding
- [ ] Read the specific User Story from `docs/prd/06-backlog-epics.md`
- [ ] Check GitHub Issue description for task details
- [ ] Verify tech stack matches PRD requirements
- [ ] Check HLD/LLD for architecture patterns (if exists)
- [ ] Confirm acceptance criteria from milestones

### Step 4: If Deviation Needed
**NEVER deviate without explicit user approval:**
1. State: "PRD specifies [X], but I'm considering [Y] because..."
2. Ask user: "Should I follow PRD or use [alternative]?"
3. Only proceed after explicit user decision

### ⚠️ VIOLATION CONSEQUENCES
Implementing non-PRD-compliant code wastes development time and requires rework.
**When in doubt, ASK the user before implementing.**

---

## 📝 REMEMBER

1. **🚨 READ PRD FIRST** - Check `docs/prd/06-backlog-epics.md` before ANY implementation
2. **Verify tech stack** - Use Keycloak, Flowable, Drools, RabbitMQ as specified
3. **Read `docs/workflow/SDLC.md`** for full process details
4. **Check GitHub issues first** before starting work
5. **Update issues as you go** - don't wait until the end
6. **On sprint approval** - create ALL GitHub artifacts automatically
7. **Keep this file updated** after each feature completion
8. **Bugs → GitHub Issue first**, then fix (never skip issue creation)
9. **Plan file → Current task only** (clear after completion)
10. **Git → Feature branch per task**, merge only after UAT approval
