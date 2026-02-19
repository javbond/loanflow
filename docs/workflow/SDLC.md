# LoanFlow - Software Development Life Cycle (SDLC)

## Overview
This document defines the complete software development workflow for LoanFlow, a Loan Origination System for Indian Banks. It follows the **Hybrid Agile Approach** - full backlog upfront with sprint-based refinement.

---

## ⚠️ CRITICAL: SPRINT APPROVAL AUTOMATION

**When user approves a sprint plan, Claude MUST automatically:**

1. **Create GitHub Milestone** for the sprint
2. **Create GitHub Issues** for each:
   - Epic (if not already exists)
   - User Story (with full acceptance criteria)
3. **Link all issues** to the milestone
4. **Add issues to Project Board** (Backlog → Todo column)
5. **Save sprint plan** to `docs/sprints/sprint-N-plan.md`
6. **Update CLAUDE.md** with current sprint status
7. **Confirm with URLs** of all created artifacts

**This is NOT optional - all items must be created on approval.**

---

## 🎯 WORKFLOW OVERVIEW

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LOANFLOW DEVELOPMENT WORKFLOW                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌───────────┐ │
│  │  MILESTONE   │───▶│    EPIC      │───▶│ USER STORY   │───▶│   TASK    │ │
│  │  (Quarterly) │    │  (Sprint)    │    │  (Sprint)    │    │  (Daily)  │ │
│  └──────────────┘    └──────────────┘    └──────────────┘    └───────────┘ │
│         │                   │                   │                   │       │
│         ▼                   ▼                   ▼                   ▼       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌───────────┐ │
│  │   ROADMAP    │    │   SPRINT     │    │     TDD      │    │    UAT    │ │
│  │   PLANNING   │    │   PLANNING   │    │ DEVELOPMENT  │    │  TESTING  │ │
│  └──────────────┘    └──────────────┘    └──────────────┘    └───────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 PHASE 1: MILESTONE & ROADMAP PLANNING

### Milestone Definition
**Location:** `docs/prd/07-milestones-roadmap.md` + GitHub Milestones

| Milestone | Duration | Focus | Success Criteria |
|-----------|----------|-------|------------------|
| M1: Core Platform | Sprint 1-2 | Foundation & CRUD | Auth, Customer, Loan APIs working |
| M2: Policy Engine | Sprint 3-4 | Business Rules | Dynamic policies, workflow |
| M3: Integrations | Sprint 5 | External Systems | Credit bureau, e-KYC |
| M4: Production | Sprint 6 | Go-Live | UAT passed, security audit |

### GitHub Commands
```bash
# Create milestone
gh api repos/:owner/:repo/milestones -f title="M1: Core Platform" -f due_on="2026-03-31"

# List milestones
gh api repos/:owner/:repo/milestones --jq '.[] | "\(.number): \(.title) - \(.state)"'

# Close milestone
gh api repos/:owner/:repo/milestones/1 -X PATCH -f state="closed"
```

---

## 📋 PHASE 2: BACKLOG MANAGEMENT

### Backlog Hierarchy
```
PRODUCT BACKLOG (docs/prd/06-backlog-epics.md)
│
├── EPIC-001: Platform Foundation
│   ├── US-001: Project Setup (8 pts)
│   ├── US-002: Authentication (5 pts)
│   └── US-003: RBAC (5 pts)
│
├── EPIC-002: Loan Application
│   ├── US-004: Customer Management (8 pts)
│   ├── US-005: Loan Application UI (8 pts)
│   └── US-006: Document Management (5 pts)
│
└── EPIC-003: Policy Engine (Future)
```

### Priority Labels
| Priority | Label | Response |
|----------|-------|----------|
| P0 | Critical | This sprint |
| P1 | High | Next sprint |
| P2 | Medium | Backlog |
| P3 | Low | Nice to have |

### GitHub Commands
```bash
# Create Epic
gh issue create --title "[EPIC-XXX] Title" --label "epic,P0" --milestone "M1"

# Create User Story
gh issue create --title "[US-XXX] Title" --label "user-story,P0,sprint-1" --milestone "Sprint 1"

# View backlog by priority
gh issue list --label "P0" --state open
```

---

## 📋 PHASE 3: SPRINT PLANNING

### Sprint Structure
```
SPRINT DURATION: 2 weeks (10 working days)

Day 1 (Monday):     Sprint Planning (4 hours)
Day 2-9:            Development + Daily Standup (15 min)
Day 10 (Friday):    Sprint Review (2h) + Retrospective (1.5h)
```

### Sprint Planning Workflow

```
USER: "Start sprint planning" OR "/scrum-sprint planning"
CLAUDE:
  1. Review completed sprints: ls docs/sprints/
  2. Determine sprint number (N+1)
  3. Read backlog: docs/prd/06-backlog-epics.md
  4. Fetch open issues: gh issue list --state open
  5. Propose sprint goal & committed stories
  6. Show task breakdown with estimates
  7. ASK FOR APPROVAL

USER: "Approved"
CLAUDE (ON APPROVAL - MUST DO ALL):
  1. CREATE MILESTONE: gh api repos/:owner/:repo/milestones -f title="Sprint N"
  2. CREATE EPIC ISSUES (if not exist)
  3. CREATE USER STORY ISSUES (for each committed story)
  4. ADD TO PROJECT BOARD
  5. SAVE SPRINT PLAN: docs/sprints/sprint-N-plan.md
  6. UPDATE CLAUDE.md
  7. CONFIRM TO USER with all URLs
```

### Example Sprint Approval Output
```
USER: "Approved"

CLAUDE: Creating Sprint 3 in GitHub...

✅ Milestone Created:
   https://github.com/user/loanflow/milestone/6

✅ Epic Created:
   #20 [EPIC-003] Policy Engine

✅ User Stories Created:
   #21 [US-008] Policy Data Model (5 pts)
   #22 [US-009] Policy Builder UI (8 pts)

✅ Added to Project Board: LoanFlow Development Board

✅ Sprint Plan Saved: docs/sprints/sprint-3-plan.md

Sprint 3 is ready!
```

---

## 📋 PHASE 4: DAILY DEVELOPMENT (TDD)

### TDD Development Cycle
```
    ┌─────────┐      ┌─────────┐      ┌──────────┐
    │  RED    │─────▶│  GREEN  │─────▶│ REFACTOR │
    │ (Test)  │      │ (Code)  │      │ (Clean)  │
    └─────────┘      └─────────┘      └──────────┘
```

### Feature Development Steps
1. Check GitHub issue for acceptance criteria
2. Update issue status: `gh issue edit #XX --add-label "in-progress"`
3. **Backend:** Write TDD test → Implement → Refactor
4. **Frontend:** Create models → Service → Components
5. Update GitHub issue with progress
6. On completion: `gh issue close #XX --comment "✅ Completed"`

### Branching Strategy
```bash
# Feature branch
git checkout -b feature/US-XXX-short-description

# Commit format (Conventional Commits)
git commit -m "feat(customer): add KYC verification endpoint

Closes #XX"
```

### Code Quality Gates
| Check | Tool | Threshold |
|-------|------|-----------|
| Unit Tests | JUnit/Jasmine | >80% coverage |
| Code Quality | SonarQube | 0 bugs, 0 vulnerabilities |
| Security | OWASP, Snyk | No high/critical |

---

## 📋 PHASE 5: CODE REVIEW & PR

### Pull Request Workflow
1. Push feature branch
2. Create PR: `gh pr create --title "[US-XXX] Feature" --body "..."`
3. CI Pipeline runs (build, test, security scan)
4. 2 reviewers approve
5. Squash merge to develop
6. Auto-deploy to staging

---

## 📋 PHASE 6: MANUAL UAT TESTING

### UAT Workflow
```
USER: "Test [feature]" OR "UAT for Epic X"
CLAUDE:
  1. Ensure services running (UAT profile)
  2. Guide through test scenarios
  3. On success: Close GitHub issue
  4. On failure: Create bug issue, fix, retest
```

### UAT Test Case Template
```
TEST CASE: TC-001 - Create New Customer
─────────────────────────────────────────
STEPS:
1. Click "New Customer" button
2. Fill form with test data
3. Click "Save"

EXPECTED: Customer created, redirected to detail page

STATUS: ✅ Pass / ❌ Fail
```

### Start UAT Environment
```bash
# Infrastructure
cd infrastructure && docker-compose up -d

# Backend (UAT mode - security disabled)
cd backend/customer-service && mvn spring-boot:run -Dspring-boot.run.profiles=uat &
cd backend/loan-service && mvn spring-boot:run -Dspring-boot.run.profiles=uat &

# Frontend
cd frontend/loanflow-web && ng serve
```

---

## 📋 PHASE 7: SPRINT REVIEW & RETROSPECTIVE

### Sprint Review
```
USER: "/scrum-sprint review"
CLAUDE:
  1. Gather metrics from GitHub
  2. Calculate velocity
  3. Generate docs/sprints/sprint-N-review.md
  4. Close milestone
```

### Retrospective
```
USER: "/scrum-sprint retro"
CLAUDE:
  1. What went well
  2. What didn't go well
  3. Action items for next sprint
```

---

## 📋 PHASE 8: RELEASE & DEPLOYMENT

### Release Workflow
1. All sprint items completed
2. UAT passed
3. Create release: `gh release create v1.0.0 --generate-notes`
4. Deploy to staging (auto)
5. Smoke test
6. Deploy to production (manual approval)

---

## 📊 GITHUB PROJECT BOARD

### Board Columns
```
┌──────────┬──────────┬─────────────┬──────────┬──────────┐
│ BACKLOG  │   TODO   │ IN PROGRESS │  REVIEW  │   DONE   │
├──────────┼──────────┼─────────────┼──────────┼──────────┤
│ Future   │ Sprint   │ Currently   │ PR/UAT   │ Completed│
│ items    │ committed│ working     │ pending  │ items    │
└──────────┴──────────┴─────────────┴──────────┴──────────┘
```

### Automation Rules
| Trigger | Action |
|---------|--------|
| Issue created | → Backlog |
| Issue assigned to milestone | → Todo |
| Label "in-progress" added | → In Progress |
| PR created | → Review |
| Issue closed | → Done |

---

## 🔧 CLAUDE COMMANDS REFERENCE

| Command | Phase | Action |
|---------|-------|--------|
| `/agile-backlog` | Backlog | Create epics/stories/tasks |
| `/scrum-sprint planning` | Sprint | Plan new sprint |
| `/scrum-sprint progress` | Sprint | Daily status |
| `/scrum-sprint review` | Sprint | End of sprint review |
| `/scrum-sprint retro` | Sprint | Retrospective |
| `/scrum-sprint backlog` | Backlog | View/prioritize backlog |

---

## ✅ DEFINITION OF DONE

- [ ] Code compiles without errors
- [ ] Unit tests >80% coverage
- [ ] No critical SonarQube issues
- [ ] Manual UAT passed
- [ ] GitHub issue closed with comment
- [ ] Sprint plan updated
- [ ] CLAUDE.md updated

---

## 📂 FILE STRUCTURE

```
loanflow/
├── CLAUDE.md                     # AI development instructions
├── .claudeignore                 # Files to ignore for context
├── docs/
│   ├── workflow/
│   │   └── SDLC.md               # This document
│   ├── prd/
│   │   ├── 06-backlog-epics.md   # Product backlog
│   │   └── 07-milestones-roadmap.md
│   ├── sprints/
│   │   ├── sprint-1-plan.md
│   │   ├── sprint-1-review.md
│   │   └── sprint-2-plan.md
│   └── project-status.md
├── backend/                      # Spring Boot services
├── frontend/                     # Angular app
└── infrastructure/               # Docker, K8s configs
```
