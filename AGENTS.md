# AGENTS.md

This file guides coding agents (LLMs) working in this repository. Follow these instructions to make consistent, testable, and review-friendly changes.

## Project Overview

- This is a core plugin for the Minecraft server PlutoProject, with logic for both Paper and Velocity platforms.
- Languages: Java & Kotlin. Always use Kotlin unless necessary.
- Tech Stack: Gradle, Paper API, Velocity API, Kotlin Compose, MongoDB, etc.

## General Rules

- Parallelize tasks when appropriate.
- If a user request is unclear or lacks key information, ask for clarification before coding.
  - Do the same for any uncertainties about the codebase.
- For bug fixes, do a full investigation first: locate the entrypoint, trace the call chain, and search usages across modules. Do not "guess and patch".
- Testing:
  - Legacy modules (`framework-*`, `feature-*`) have no unit tests, and unit tests are not required.
  - For legacy modules, do NOT add unit tests to validate changes.
  - For new-structure features under `features/`, unit tests are part of the design; write them when developing.
  - After implementing something, run `./gradlew shadowJar` to confirm successful compilation.
  - After completing a task for new-structure features, run `./gradlew test` to ensure all tests pass.
    - If tests fail, locate the issue from the test output and fix the code.
    - Do not change or delete tests just to make them pass (unless the test itself clearly expresses the wrong rule, which should be rare).
  - For gameplay-related features, tell the user how to test it and describe the expected outcome, so they can manually verify and report back.
- Commit messages: Use Conventional Commits and write in Chinese. Follow the existing commit style.

## Project Structure

- The `paper`/`velocity` in Gradle module names indicates on which platform the code here will run. `common` means the code is cross-platform and does not rely on any platform-specific APIs.
- All Gradle modules starting with `framework` contain code that "supports other features and is not directly perceived by players."
- All Gradle modules starting with `feature` contain code that "provides actual gameplay features for players to experience."
- The project is evolving towards a new structure. New Features should be placed under the `features/` directory.
  - The new structure is designed for: testability, portability, readability, and low coupling.
  - The new structure follows Domain-Driven Design (DDD) conventions and typically consists of these modules:
    - `core`: Combines the traditional DDD Application + Domain layers.
      - Do not put any Bukkit dependencies or external infrastructure dependencies here (e.g., MongoDB). This module may only depend on Kotlin itself (including coroutines and serialization).
      - You do not need to explicitly draw a strict boundary between Domain and Application internally, but keep naming distinct and intentional, e.g. `GrantWhitelistUseCase` / `RevokeWhitelistUseCase` (Application use cases), `WhitelistRecord` (Domain object).
      - Calls to external infrastructure / platform capabilities must be abstracted as ports (e.g., `XXXPort`) or repositories (database special case: `XXXRepository`), implemented by real `adapter` / `infra` modules.
      - For database repository interfaces, use Domain objects directly as parameters. Convert between Domain objects and database models inside the repository implementation (typically via mapper utilities in the corresponding infra module, e.g. `MongoMappers.kt`).
      - For use cases with multiple business outcomes, return an explicit result type. Do not use `Boolean` to collapse all failures into `false`.
      - Unless a use case is only "complete" after performing a platform/infrastructure operation (e.g., deducting currency, applying potion effects), do not invoke those operations in the core logic (typical examples: sending chat messages, Titles). Instead, adapters should perform those operations based on the use case result.
      - This layer is designed to allow unit tests, and in practice you should write them.
    - `infra-mongo`: MongoDB infrastructure layer.
      - Store the actual serialized/persisted data model classes and repository implementations here.
      - Data model classes must use the `XXXDocument` naming convention.
      - This layer usually needs unit tests, using TestContainers to verify real database operations.
    - `infra-messaging`: CharonFlow infrastructure layer.
      - Store concrete message types and message send/receive ports, for `core` to use.
      - Only create a messaging port when message delivery is required for the business to be considered complete; otherwise, keep messaging operations / message classes in the adapter layer.
      - CharonFlow is an internal Redis-based communication framework not included in this repository. If you need to integrate with it, search the codebase for existing usage examples.
      - If you are still uncertain after searching the codebase, ask the user via your built-in tools; do not guess.
      - Whether this layer needs unit tests depends on the scenario; if unsure, ask the user.
    - `api`: The outward-facing API for this feature.
      - The API must act as an inbound adapter / facade: the API layer calls use cases / domain objects in `core` (never the other way around).
        - `core` must not depend on the `api` module.
      - The API layer must define its own interface/entity classes and must not expose `core` domain objects / use case classes to external callers.
        - Even if the API interface types are almost identical to core domain objects, still define a separate API version instead of reusing core classes.
      - The API implementation lives in `adapter-common`.
        - Put implementations under an `impl` package, e.g. `plutoproject.feature.whitelist_v2.adapter.common.impl`.
      - This layer must not depend on any platform-specific (Bukkit/Velocity) or infrastructure classes.
        - If you need platform-specific APIs, create `api-paper` / `api-velocity` modules. These do not need to be unit-testable and may call platform/infrastructure code.
        - Implement platform-specific APIs in `adapter-paper` / `adapter-velocity`, following the same `impl` package convention.
    - `adapter-*`: Feature adapters. This includes platform-driven events/commands and also implementations for the API layer.
      - `adapter-common`: Cross-platform logic.
      - `adapter-paper`: Paper-specific logic. Paper feature entrypoints live here.
      - `adapter-velocity`: Velocity-specific logic. Velocity feature entrypoints live here.
      - Most dual-platform features are not simple "same logic twice"; they often involve proxy <-> backend communication, so expect platform-specific responsibilities (e.g., proxy sends messages, backend receives messages).
  - Any newly developed feature modules should follow the new structure and be implemented using it.
    - When implementing a new-structure feature, use the `whitelist-v2` feature module as the reference standard.
  - Existing feature modules remain under the legacy `feature-*` modules unless migration/refactor is explicitly requested. Legacy features are not deprecated and will coexist with the new structure for a long time.
  - `framework-*` modules have no migration plan and remain as-is.
- All artifact IDs and versions for dependencies are managed via Gradle's Version Catalog feature in `gradle/libs.version.toml`.

### Protected Directories & Files

**Never edit these files or directories unless explicitly instructed:**

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

Creating new source files under Gradle module `features/` or `framework-*/` is allowed and preferred over editing unrelated modules.

## Useful Commands

- **Build & Package Plugin**: `./gradlew shadowJar`
- **Run Tests**: `./gradlew test`
- **Clean Cache**: `./gradlew clean` (use if you encounter unexplained build errors not caused by your code).

## Dependency Rules

- Do not upgrade Gradle, Kotlin, or any other dependency versions unless explicitly instructed.
- Do not add unnecessary dependencies for simple logic. First, check if there is an existing implementation; if not, implement it yourself.
  - Examples:
    - Requirements such as base64 encoding, hashing, or Fibonacci sequence calculation can be implemented with a simple function and do not require additional dependencies.
    - Requirements such as sending data packets to players or displaying advanced particle effects may appropriately rely on external libraries.
- If introducing a dependency is necessary to fulfill a requirement, always ask the user to confirm whether it is needed.
- When adding a dependency, determine its usage scenario.
  - For dependencies related to Minecraft server logic (such as CoreProtect API, Cloud Command Framework, etc.), add them to the corresponding platform: `build-logic/src/main/kotlin/plutoproject.<platform>-conventions.gradle.kts`.
  - For dependencies used throughout the entire project and unrelated to Minecraft server logic, add them to: `build-logic/src/main/kotlin/plutoproject.base-conventions.gradle.kts`.
  - Always adhere to the principle of minimal dependencies; do not expose dependencies to Gradle modules that do not need them.
