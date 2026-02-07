# DDD + TDD Refactor Plan (Whitelist V2 as Template)

## Goal

Introduce a new hierarchical Gradle module layout for new features:

- Project path format: `:feature:<name>:<layer>` (dash-separated)
- First migrated feature: `whitelist-v2`
- Old layout (`feature-*`, `framework-*`) stays as-is and continues to build.
- For a long transition period, both paradigms co-exist.

This document is both:

- the implementation plan (what to change, where, in which order)
- the shared TODO list (for both the agent and the maintainer)

## Scope / Non-Goals

In scope (for `whitelist-v2` only):

- New modules + wiring
- DDD layering (api/core/infra/paper/velocity)
- Unit tests for `core`
- Integration tests (Testcontainers) for infra persistence

Out of scope (for now):

- Refactoring existing `feature-*` or `framework-*` modules
- Forcing unit tests for old modules
- Reworking the framework feature loader / global environment (can be tackled later)

## Target Module Layout (Whitelist V2)

New modules to add:

- `:feature:whitelist-v2:api`
- `:feature:whitelist-v2:core`
- `:feature:whitelist-v2:infra-mongo`
- `:feature:whitelist-v2:infra-messaging`
- `:feature:whitelist-v2:paper`
- `:feature:whitelist-v2:velocity`

Directory convention (recommended):

- `feature/whitelist-v2/api`
- `feature/whitelist-v2/core`
- `feature/whitelist-v2/infra-mongo`
- `feature/whitelist-v2/infra-messaging`
- `feature/whitelist-v2/paper`
- `feature/whitelist-v2/velocity`

## Dependency Direction (DDD-friendly)

- `api` contains stable contracts (no Koin, no Mongo, no Paper/Velocity)
- `core` implements business rules + use-cases, depends on `api`
- `infra-*` implements `core` ports (Mongo/messaging/etc)
- `paper`/`velocity` are adapters, depend on `api` + `core` + needed `infra-*`

Suggested dependency graph:

```text
:feature:whitelist-v2:api
            ^
            |
:feature:whitelist-v2:core
            ^
            |
  +---------+-----------------+
  |                           |
infra-mongo              infra-messaging
  ^                           ^
  |                           |
paper                     velocity
```

## Conventions (Build Logic)

Keep existing conventions untouched for old modules.

Add two new conventions for the new feature layout:

- `plutoproject.core-conventions`
  - For minimal, JVM-only modules (no Paper/Velocity/Mongo/Koin/Compose assumptions)
  - Lightweight JVM/Kotlin settings
  - Does NOT apply KSP/KAPT by default
  - Does NOT auto-add the large compileOnly/runtimeDownload dependency set from `plutoproject.base-conventions`

- `plutoproject.test-conventions`
  - For any modules that run tests (unit or integration)
  - Enables JUnit5 (`useJUnitPlatform()`)
  - Adds baseline test dependencies:
    - JUnit5
    - Testcontainers (core + junit-jupiter); add specific containers (e.g. MongoDB) per module when needed

## Version Catalog Additions

Add (at minimum) to `gradle/libs.versions.toml`:

- JUnit5 (jupiter-api, jupiter-engine, jupiter-params)
- Testcontainers (junit-jupiter, mongodb; add redis later if needed)

## Migration Strategy (Co-existence)

During the transition, `platform-*` can keep depending on the old aggregator modules.

Recommended wiring approach:

- `feature-paper` adds `api(project(":feature:whitelist-v2:paper"))`
- `feature-velocity` adds `api(project(":feature:whitelist-v2:velocity"))`

This way:

- old feature modules keep working
- new whitelist-v2 feature is delivered via the old aggregators
- platform modules do not need immediate dependency changes

## What Changes Inside Whitelist V2 (Key Design Points)

- Remove container-bound static access from API:
  - `feature-common-api/.../Whitelist.kt` currently uses `companion object : Whitelist by Koin.get()`
  - In the new `:feature:whitelist-v2:api`, the API must NOT depend on Koin/framework

- Move business flow out of Koin components:
  - Existing `WhitelistImpl` mixes: use-cases + persistence + mapping + time + hooks + visitor session state
  - Target: `core` owns use-cases; infra owns persistence; adapters own platform state

- Prefer explicit DI:
  - Avoid `Koin.get()` / `inject()` in core
  - Paper/Velocity adapters can still use Koin, but should inject the API service, not cast to implementation

## TODO (Execution Checklist)

Status markers:

- `[ ]` pending
- `[x]` done

### Bootstrap

- [x] DDD-000 Create and switch to a refactor branch
- [x] DDD-001 Add this `DDD_REFACTOR.md`

### Build Conventions + Catalog

- [x] DDD-010 Add `build-logic/src/main/kotlin/plutoproject.core-conventions.gradle.kts`
- [x] DDD-011 Add `build-logic/src/main/kotlin/plutoproject.test-conventions.gradle.kts`
- [x] DDD-012 Add JUnit5 + Testcontainers to `gradle/libs.versions.toml`

### Create Modules (Empty First)

- [x] DDD-020 Update `settings.gradle.kts` to include new projects for whitelist-v2
- [x] DDD-021 Create `build.gradle.kts` for each new whitelist-v2 module
- [x] DDD-022 Verify `./gradlew shadowJar` still succeeds (no code moved yet)

### Migrate Code: api/core/infra

- [ ] DDD-030 Move whitelist-v2 API types into `:feature:whitelist-v2:api` and remove Koin static access
- [ ] DDD-031 Implement `:feature:whitelist-v2:core` (ports + use-cases + JUnit5 unit tests)
- [ ] DDD-032 Move Mongo models/repos/index wiring into `:feature:whitelist-v2:infra-mongo` (+ Testcontainers tests)
- [ ] DDD-033 Move messaging DTO/topic into `:feature:whitelist-v2:infra-messaging`

### Migrate Code: adapters

- [ ] DDD-040 Move Paper whitelist-v2 code into `:feature:whitelist-v2:paper` and rewire DI to use `api/core`
- [ ] DDD-041 Move Velocity whitelist-v2 code into `:feature:whitelist-v2:velocity` and rewire DI to use `api/core`

### Co-existence Wiring

- [ ] DDD-050 Make old aggregators depend on new modules:
  - `feature-paper` -> `:feature:whitelist-v2:paper`
  - `feature-velocity` -> `:feature:whitelist-v2:velocity`

### Cleanup + Verification

- [ ] DDD-060 Remove old whitelist-v2 sources from `feature-common`, `feature-paper`, `feature-velocity`, `feature-common-api`
- [ ] DDD-061 Run `./gradlew shadowJar` to ensure compilation
- [ ] DDD-062 Run unit tests for `:feature:whitelist-v2:core`
- [ ] DDD-063 Run integration tests for `:feature:whitelist-v2:infra-mongo`

## Notes for Manual Verification (Gameplay)

After the migration, manually verify on a test server:

- Velocity: whitelist grant/revoke/lookup/visitor-mode
- Paper: visitor restrictions (chat/spectate/teleport/advancement), notification subscription
