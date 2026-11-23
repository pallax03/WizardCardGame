# AGENTS Guide (Technical Report - `docs` Branch)
This orphan branch (`docs`) hosts the **Software Process Engineering (SPE) Technical Report** built with **Astro Starlight** (`@astrojs/starlight` + `starlight-theme-terminal`), deployed to `https://pallax03.github.io/WizardCardGame/`.

> **Role**: Technical writer and documentation maintainer for the WizardCardGame SPE report.

## 1. Branch Scope & Isolation
- This branch (`docs`) contains **only** the project technical report and static site configuration (GitHub Pages).
- Never add application source code (`wizardEngine/`, `application/`) to this branch. 
  Load assets from the `master` branch, such as `wizard_logo.svg` and `icon.svg` directly from raw GitHub URLs on `master`.

## 2. Writing Style & Anti-Fluff Rules (MANDATORY)
1. **Be Direct and Concise**: Go straight to the technical point. Avoid introductory fluff, marketing adjectives, or dramatic narrative
2. **Concrete Over Descriptive**: Explain *what* was chosen and *why* (trade-offs), using short paragraphs, bullet points, and tables or Mermaid diagrams over walls of text.
3. **Strict Reality Check**: Document **only** the DDD models, Bounded Contexts, and CI/CD pipelines that actually exist or are currently planned. Never invent features, metrics, or tools not approved by the author.
4. **Ubiquitous Language Consistency**: Use the exact domain terms defined in the glossary and Scala code (1 concept = 1 interface/type).

## 3. Exam Checklist Coverage
Ensure the report clearly covers and motivates:
- **Requirements**: Functional/non-functional categorization, user stories, and Definition of Done (DoD).
- **Domain-Driven Design**: Domain, Subdomains, Bounded Contexts, Ubiquitous Language, Entities, Value Objects, Aggregates, Repositories, Services, Factories, and Hexagonal Architecture.
- **Multi-Platform**: Target platforms partitioning (JVM / Node.js) and design coherence.
- **DevOps & Versioning**: DVCS workflow, Conventional Commits, automated Semantic Versioning, containerization, and CI/CD release pipelines.
- **Quality Assurance**: Unit/integration/system testing, test coverage metrics, and static analysis.
- **Governance**: License choice motivation and GenAI/Agents exploitation disclosure.

## 4. Verification & Build
- Follow Conventional Commits with `docs` type: `docs(<section>): <imperative summary>`.
- Install dependencies: `npm ci`
- Local dev server: `npm run dev`
- Production static build (outputs to `./dist`): `npm run build`
