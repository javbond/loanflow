# LoanFlow Project - Claude Code Instructions

## Project Overview
**LoanFlow** - Loan Origination System for Indian Banks
- Multi-tenant SaaS platform
- Spring Boot 3.2 microservices + Angular 17 frontend
- TDD approach with 121+ tests

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

### Sprint 2 (Loan & Document Management) - 🔄 IN PROGRESS
**Milestone**: [Sprint 2](https://github.com/javbond/loanflow/milestone/5)
**Duration**: 2026-02-19 to 2026-03-05

| Issue | Title | Points | Status |
|-------|-------|--------|--------|
| #11 | [EPIC-002] Loan Application Management | - | 🔄 Parent |
| #14 | [US-005] Loan Application Angular UI | 8 | ✅ Done |
| #15 | [US-006] Document Management Angular UI | 5 | ✅ Done |

**Bug Fixes (Sprint 2):**
| Issue | Description | Status |
|-------|-------------|--------|
| #16 | Document Upload UUID mismatch | ✅ Closed |
| #17 | MinIO credentials mismatch | ✅ Closed |
| #18 | Document verification verifierId | ✅ Closed |
| #19 | Documents list not visible | ✅ Closed |

**Progress:** `████████░░` 80% (Loan + Document UI complete, UAT verified)

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
| #3 | [US-002] Authentication System | P1 | Sprint 3 |
| #4 | [US-003] Role-Based Access Control | P1 | Sprint 3 |
| #12 | [EPIC-003] Policy Engine | P2 | Sprint 4 |

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
| loan-service | 8081 | 27 | ✅ Done | ✅ Done |
| document-service | 8083 | 49 | ✅ Done | ✅ Done |
| notification-service | 8084 | - | ⏳ Pending | ⏳ Pending |
| api-gateway | 8080 | - | ⏳ Pending | - |

**Total TDD Tests**: 121

---

## 🚀 QUICK START

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

## 📝 REMEMBER

1. **Read `docs/workflow/SDLC.md`** for full process details
2. **Check GitHub issues first** before starting work
3. **Update issues as you go** - don't wait until the end
4. **On sprint approval** - create ALL GitHub artifacts automatically
5. **Keep this file updated** after each feature completion
6. **Bugs → GitHub Issue first**, then fix (never skip issue creation)
7. **Plan file → Current task only** (clear after completion)
8. **Git → Feature branch per task**, merge only after UAT approval
