---
version: "0.1.2"
level: pair
processes:
  ideation: none
  requirements-analysis: hint
  architecture-design: hint
  implementation: pair
  testing: pair
  ci-cd-configuration: assist
  documentation-writing: assist
components:
  wizardEngine/src/main/scala/*: pair
  wizardEngine/src/test/*: pair
  application/*: copilot
  .github/workflows/*: assist
---

## Notes

- **Research & DDD Brainstorming (`hint`)**: AI is used to discover third-party libraries and brainstorm Domain-Driven Design (Bounded Contexts, Aggregates, and Ubiquitous Language). Final architectural choices are human-made.
- **Code & Tests (`pair` / `copilot`)**: AI generates initial code drafts and test boilerplate. All generated output is manually refactored, debugged, and cleaned up to enforce strict DDD and Hexagonal boundaries.
- **Tooling & Docs (`assist`)**: Used for GitHub Actions syntax, build scripts (`sbt`/`npm`), and documentation grammar checks.