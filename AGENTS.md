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
- Testing:
  - This project has no unit tests, and unit tests are not required.
  - Do NOT run or add unit tests to validate changes.
  - After implementing something, run `./gradlew shadowJar` to confirm successful compilation.
  - For gameplay-related features, tell the user how to test it and describe the expected outcome, so they can manually verify and report back.
- Commit messages: Use Conventional Commits and write in Chinese. Follow the existing commit style.

## Project Structure

- The `paper`/`velocity` in Gradle module names indicates on which platform the code here will run. `common` means the code is cross-platform and does not rely on any platform-specific APIs.
- All Gradle modules starting with `framework` contain code that "supports other features and is not directly perceived by players."
- All Gradle modules starting with `feature` contain code that "provides actual gameplay features for players to experience."
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

Creating new source files under Gradle module `feature-*/` or `framework-*/` is allowed and preferred over editing unrelated modules.

## Useful Commands

- **Build & Package Plugin**: `./gradlew shadowJar`
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
