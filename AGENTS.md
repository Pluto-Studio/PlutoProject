# AGENTS.md

Guidance for coding agents (LLMs) in this repo; follow to keep changes consistent, testable, and review-friendly.

## Overview

- PlutoProject Minecraft server core plugin; supports Paper + Velocity.
- Languages: Java + Kotlin; prefer Kotlin unless necessary.
- Stack: Gradle, Paper API, Velocity API, Kotlin Compose, MongoDB, etc.

## Workflow Rules

- Parallelize tasks when appropriate.
- If the request or codebase is unclear/missing key info: ask before coding.
- Bug fixes: investigate first (entrypoint -> call chain -> cross-module usages); no "guess and patch".

## Testing Rules

- Legacy modules (`framework-*`, `feature-*`): no unit tests; do NOT add unit tests.
- New-structure features under `feature/`: unit tests are part of the design; write them.
- After implementing: run `./gradlew shadowJar` to confirm compilation.
- After completing new-structure work: also run `./gradlew test`.
  - If tests fail: fix based on output.
  - Do not change/delete tests just to pass (unless the test itself is clearly wrong; rare).
- Gameplay changes: tell the user how to manually test and the expected outcome.

## Git Rules

- Commit messages: Conventional Commits, written in Chinese, matching existing style.

## Project Structure

- Gradle module naming: `paper`/`velocity` = platform; `common` = cross-platform (no platform-specific APIs).
- `framework*`: supporting code not directly perceived by players. `feature*`: player-facing gameplay.
- New features go under `feature/` (testability/portability/readability/low coupling); use `whitelist-v2` as the reference.
- Legacy `feature-*` stays unless migration/refactor is explicitly requested; `framework-*` has no migration plan.
- Dependency coordinates/versions are managed via Version Catalog: `gradle/libs.version.toml`.

### New-Structure DDD Modules (under `feature/`)

- `core` (Application + Domain):
  - Kotlin-only deps (incl. coroutines/serialization); no Bukkit or infra deps (e.g., MongoDB).
  - Keep naming intentional (e.g., `GrantWhitelistUseCase`/`RevokeWhitelistUseCase`, `WhitelistRecord`).
  - External/platform ops via ports (`XXXPort`) or repositories (`XXXRepository`), implemented in `adapter`/`infra` modules.
  - DB repository interfaces accept Domain objects; map Domain <-> DB models inside repo impl (e.g., `MongoMappers.kt`).
  - Use explicit result types for multi-outcome use cases (no `Boolean` collapse).
  - Avoid platform/infra side effects in core unless required for "complete" business behavior (e.g., deduct currency, teleport player); adapters react to core results.
  - Designed to be unit-testable; write tests.
- `infra-mongo`:
  - Persisted models + repo implementations live here.
  - Persisted model naming: `XXXDocument`.
  - Usually needs unit tests with TestContainers (real DB operations).
- `infra-messaging` (CharonFlow / internal Redis framework, not in this repo):
  - Concrete message types + send/receive ports for `core`.
  - Create a messaging port only when delivery is required for business completion; otherwise keep messaging in adapters.
  - Search the codebase for existing usage; if still uncertain, ask the user (do not guess).
  - Unit test needs depend on scenario; ask if unsure.
- `api` (outward-facing feature API):
  - Inbound facade: `api` calls `core` (never the reverse); `core` must not depend on `api`.
  - Define API types; do not expose `core` domain/use case types (even if identical).
  - API implementations live in `adapter-common` under an `impl` package (e.g., `plutoproject.feature.whitelist_v2.adapter.common.impl`).
  - `api` must not depend on platform/infra.
  - If platform-specific APIs are needed: create `api-paper`/`api-velocity` (not required to be unit-testable) and implement them in `adapter-paper`/`adapter-velocity` under `impl`.
- `adapter-*`:
  - Adapters include platform entrypoints (events/commands) and API implementations.
  - `adapter-common` cross-platform; `adapter-paper` Paper entrypoints; `adapter-velocity` Velocity entrypoints.
  - Dual-platform features are often not "same logic twice" (proxy <-> backend comms); expect platform-specific responsibilities.

### Protected Directories & Files

Never edit these unless explicitly instructed:

- `.github/`
- `.kotlin/`
- `.idea/`
- `.gradle/`
- `.gitignore`
- `build/`
- `LICENSE`
- `README.md`
- `AGENTS.md` (this file)
- `gradle.properties`
- `gradlew`
- `gradlew.bat`

Creating new source files under Gradle module `feature/` or `framework-*/` is allowed and preferred over editing unrelated modules.

## Useful Commands

- Build & package: `./gradlew shadowJar`
- Run tests: `./gradlew test`
- Clean cache: `./gradlew clean` (use for unexplained build errors not caused by your code)

## Dependency Rules

- Do not upgrade Gradle/Kotlin/other dependency versions unless explicitly instructed.
- Avoid unnecessary dependencies for simple logic: first check for an existing implementation; otherwise implement it.
  - Examples: base64/hashing/Fibonacci -> implement; packets/advanced particles -> external libs may be appropriate.
- If a new dependency is necessary: ask the user to confirm; keep scope minimal (only modules that need it) and be clear about the usage scenario.
