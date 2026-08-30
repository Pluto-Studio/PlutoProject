Core plugin for the PlutoProject Minecraft server.

- The project uses Kotlin and Java. Prefer Kotlin unless Java is required.
- Builds use Gradle with custom `build-logic`.
- The main dependencies are the Velocity and Paper APIs, MongoDB, and Kotlin Compose with an internal in-game menu library.

## Terms

- **Kernel:** Manages module lifecycles.
- **Module:** A runtime-loaded Capability or Feature, usually named `RuntimeModule` in code. Unless stated otherwise, "module" means this rather than a Gradle module.
- **Capability:** Provides infrastructure to other modules without adding gameplay. For example, `mongo` provides `MongoConnection`. Features may depend on capabilities, but capabilities must not depend on features.
- **Feature:** Contains gameplay features or playable systems. It may expose APIs to other modules.
- **Foundation:** Lifecycle-free utilities that support capabilities and features. Foundation code is not a module.

## Workflow rules

- Build with `./gradlew shadowJar`. Gradle writes artifacts to `build/libs/`. Run tests with `./gradlew test`.
- Do not run the full build or test suite after every change. Skip verification for trivial edits. When verification is needed, run only the build or test tasks for affected Gradle modules.
- Write Git commit messages in Chinese. Follow Conventional Commits and the repository's existing style.

## Coding conventions

- When adding a feature or capability module, add its dependency to the platform Gradle module. Otherwise, the build will not package it into the JAR.
- Put text shown to players by commands, interactions, menus, and similar features in `Messages.kt`, not at usage sites. Find existing `Messages.kt` files and follow their patterns.
- Define messages in `Messages.kt` as values, never functions. Insert dynamic content through placeholders, even where existing code uses message functions.