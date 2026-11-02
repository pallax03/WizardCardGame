# AGENTS Guide
**WizardCardGame** is a distributed multiplayer implementation of the *Wizard* trick-taking card game.

> **BIBLE**: Refer to the [SPE Technical Report](https://pallax03.github.io/WizardCardGame/report/04-process-dvcs/#43-conventional-commits--scope-taxonomy) for Bounded Contexts, Hexagonal boundaries, and Conventional Commits scopes.

## 1. Repository Structure
- `wizardEngine/`: Scala 3 Hexagonal core engine (`engine/model/`, `engine/ports/`, `engine/adapters/`), `tuProlog` AI (`src/main/resources/prolog/`), Redis adapters, and Vert.x HTTP/WebSocket verticles.
  Managed via `sbt`.
- `application/`: TypeScript/React web client, managed via `npm`.
- `.github/`: GitHub Actions CI/CD workflows, Dependabot config, and structured Issue/PR templates.

## 2. Architecture & DDD Rules
- **Domain Isolation**: `engine/model/` must contain only pure domain logic (Entities, Value Objects, Aggregates, Domain Events). No I/O, HTTP, or DB libraries allowed.
- **Minimalism (YAGNI)**: Do not add unrequested dependencies, abstractions, or boilerplate.

## 3. Git & PR Workflow
- Work on short-lived branches `<type>/<issue-id>-<slug>` linked to a single Issue.
- Make atomic commits (`<type>(<scope>): <summary>`). Never use `git add .` to bundle unrelated changes.
- If you discover an unrelated defect, SUGGEST TO:
   - **Blocker**: Pause the feature branch, fix and test the bug in a dedicated `fix/<id>-<slug>` PR merged to `master`, then rebase.
   - **Non-blocker**: Open a separate Issue via `bug_report.yml` and leave the unrelated code untouched.

## 4. Verification Commands (Mandatory Before Every Commit)
- **JVM Backend (`wizardEngine/`)**:
  - Format & static analysis: `cd wizardEngine && sbt "; scalafmtCheckAll; scalafixAll --check"`
  - Run tests with coverage: `cd wizardEngine && sbt "; coverage; test; coverageReport"`
  - Build fat JAR: `cd wizardEngine && sbt assembly`
- **Web Client (`application/`)**:
  - Install & lint: `cd application && npm ci && npm run lint`
  - Production build: `cd application && npm run build`
