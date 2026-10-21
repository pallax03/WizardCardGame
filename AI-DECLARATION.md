---
version: "0.1.2"
level: assist
processes:
  ideation: none
  requirements-analysis: none
  architecture-design: hint
  documentation-writing: assist
components:
  src/content/docs/*: assist
  src/styles/*: auto
  astro.config.mjs: assist
---

## Notes

- **Technical Report & Docs Site (`assist`)**: Static site setup and report drafting are delegated to AI starting from human-provided architectural notes and trade-offs, then manually reviewed for technical accuracy and conciseness.
- **English Translation & Lexical Refinement (`assist`)**: GenAI was used at the `assist` level to translate the author's draft notes into fluent academic English, fix grammatical errors, and improve lexical clarity without altering the underlying engineering concepts.

> **Disclaimer**: Every piece of AI-assisted text or configuration is manually reviewed, verified, and (if necessary) edited by the author before being committed.
