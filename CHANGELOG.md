# Changelog

All notable changes to this project will be documented in this file. See [Conventional Commits](https://conventionalcommits.org) for commit guidelines.

## [1.0.0](https://github.com/pallax03/WizardCardGame/releases/tag/v1.0.0) (2026-10-27)

### Features

* **domain:** implement core Wizard hexagonal domain model, ports, and lobby state ([151d352](https://github.com/pallax03/WizardCardGame/commit/151d352))
* **ai:** integrate tuProlog symbolic engine, theories, and bot adapter ([ce10942](https://github.com/pallax03/WizardCardGame/commit/ce10942))
* **codecs:** implement JSON serialization codecs for domain state and events ([1be48c2](https://github.com/pallax03/WizardCardGame/commit/1be48c2))
* **redis:** implement distributed lobby state, CAS Lua scripts, Pub/Sub, and recovery adapters ([9512fe7](https://github.com/pallax03/WizardCardGame/commit/9512fe7))
* **http:** expose Vert.x REST endpoints, OpenAPI specs, WebSockets, and bot verticles ([fe154a1](https://github.com/pallax03/WizardCardGame/commit/fe154a1))
* **ui:** add Next.js prototypal web client for distributed multiplayer sessions ([1374cc4](https://github.com/pallax03/WizardCardGame/commit/1374cc4))

### Build & Infrastructure

* **repo:** initialize multi-platform repository structure and build configs ([70a2d7c](https://github.com/pallax03/WizardCardGame/commit/70a2d7c))
* **docker:** add Dockerfiles, Caddy reverse proxy, compose, and baseline CI workflows ([3bd15e9](https://github.com/pallax03/WizardCardGame/commit/3bd15e9))
