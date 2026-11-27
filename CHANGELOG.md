## [1.2.4](https://github.com/pallax03/WizardCardGame/compare/v1.2.3...v1.2.4) (2026-11-27)


### Bug Fixes

* **util:** avoid SLF4J startup race in TestWizardLogger ([#227](https://github.com/pallax03/WizardCardGame/issues/227)) ([d227970](https://github.com/pallax03/WizardCardGame/commit/d2279701f70c45ec3f34e1f0998ec27b604c3a07)), closes [#226](https://github.com/pallax03/WizardCardGame/issues/226) [#225](https://github.com/pallax03/WizardCardGame/issues/225)

## [1.2.3](https://github.com/pallax03/WizardCardGame/compare/v1.2.2...v1.2.3) (2026-11-24)


### Bug Fixes

* **repo:** guard husky prepare script and pin engines.node to 24.x for Vercel ([#215](https://github.com/pallax03/WizardCardGame/issues/215)) ([e57786f](https://github.com/pallax03/WizardCardGame/commit/e57786f501f34cb9079a1beada7932096e55df2d)), closes [#200](https://github.com/pallax03/WizardCardGame/issues/200)

## [1.2.2](https://github.com/pallax03/WizardCardGame/compare/v1.2.1...v1.2.2) (2026-11-24)


### Bug Fixes

* **deploy:** resolve previous release tag in path filter and harden Azure deploy ([#213](https://github.com/pallax03/WizardCardGame/issues/213)) ([8f103ed](https://github.com/pallax03/WizardCardGame/commit/8f103ed702d0cbdbf7fbe336e6656782d5119ce9)), closes [#212](https://github.com/pallax03/WizardCardGame/issues/212)

## [1.2.1](https://github.com/pallax03/WizardCardGame/compare/v1.2.0...v1.2.1) (2026-11-24)


### Bug Fixes

* **application:** on game resume timer thread does not count down ([#211](https://github.com/pallax03/WizardCardGame/issues/211)) ([12770f7](https://github.com/pallax03/WizardCardGame/commit/12770f787876d4ffc88b13bb9234beaf6e5fa886)), closes [#194](https://github.com/pallax03/WizardCardGame/issues/194) [#127](https://github.com/pallax03/WizardCardGame/issues/127)

# [1.2.0](https://github.com/pallax03/WizardCardGame/compare/v1.1.0...v1.2.0) (2026-11-23)


### Features

* **http:** version REST and WebSocket routes under v1 with API specifications ([#199](https://github.com/pallax03/WizardCardGame/issues/199)) ([28823c8](https://github.com/pallax03/WizardCardGame/commit/28823c88864a6a4c8a9a05582619389db7bd5970)), closes [#159](https://github.com/pallax03/WizardCardGame/issues/159)

# [1.1.0](https://github.com/pallax03/WizardCardGame/compare/v1.0.3...v1.1.0) (2026-11-23)


### Features

* **ui:** add report and GitHub links to home page and update report URLs ([#198](https://github.com/pallax03/WizardCardGame/issues/198)) ([e67f2b0](https://github.com/pallax03/WizardCardGame/commit/e67f2b071110bb4ebdc3634696fb983d04f6e6db)), closes [#180](https://github.com/pallax03/WizardCardGame/issues/180)

## [1.0.3](https://github.com/pallax03/WizardCardGame/compare/v1.0.2...v1.0.3) (2026-11-21)


### Bug Fixes

* **bot:** recover bot stream consumers and handle worker node outages ([#195](https://github.com/pallax03/WizardCardGame/issues/195)) ([e0c6e36](https://github.com/pallax03/WizardCardGame/commit/e0c6e361755b6714297b0a88ff19edc4fab56d12)), closes [#117](https://github.com/pallax03/WizardCardGame/issues/117) [#193](https://github.com/pallax03/WizardCardGame/issues/193)

## [1.0.2](https://github.com/pallax03/WizardCardGame/compare/v1.0.1...v1.0.2) (2026-11-20)


### Bug Fixes

* **repo:** run lint-staged ESLint inside application directory ([#193](https://github.com/pallax03/WizardCardGame/issues/193)) ([1baaf4e](https://github.com/pallax03/WizardCardGame/commit/1baaf4e20e86c36ca5b58e17fcb074a963dfd050)), closes [#192](https://github.com/pallax03/WizardCardGame/issues/192)

## [1.0.1](https://github.com/pallax03/WizardCardGame/compare/v1.0.0...v1.0.1) (2026-11-19)


### Bug Fixes

* **model:** make SpecialCard ID generation deterministic in Deck and tests ([#186](https://github.com/pallax03/WizardCardGame/issues/186)) ([3769c6b](https://github.com/pallax03/WizardCardGame/commit/3769c6b41ffb2b16d4b8fba30fe456dba54caab7)), closes [#179](https://github.com/pallax03/WizardCardGame/issues/179) [#161](https://github.com/pallax03/WizardCardGame/issues/161) [#160](https://github.com/pallax03/WizardCardGame/issues/160) [#176](https://github.com/pallax03/WizardCardGame/issues/176)

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
