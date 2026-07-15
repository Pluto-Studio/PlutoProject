Core plugin for the PlutoProject Minecraft server.

- Kotlin and Java; prefer Kotlin unless Java is required.
- Gradle with custom `build-logic`.
- Velocity/Paper APIs, MongoDB, and Kotlin Compose with an internal in-game menu library.

## Terms

- **Kernel**: Infrastructure that manages module lifecycles.
- **Module**: A runtime-loaded functional unit, either a Capability or Feature; usually named `RuntimeModule` in code. "Module" means this by default, not a Gradle module; Gradle modules are stated explicitly.
- **Capability**: A module that exposes infrastructure to other modules without gameplay, such as `mongo` providing `MongoConnection`. Features may depend on Capabilities; Capabilities must not depend on Features.
- **Feature**: A module containing gameplay features or playable systems. It may expose APIs to other modules.
- **Foundation**: Lifecycle-free utility functions and classes supporting Capabilities and Features. It is not a module.

## Workflow Rules

- Do not design or write tests unless explicitly requested.
- Build with `./gradlew shadowJar`; artifacts are written to `build/libs/`. Run tests with `./gradlew test`.
- Do not run a full build or test suite after every change. Skip verification for trivial edits; when needed, run build or test tasks only for affected Gradle modules.
- Write Git commit messages in Chinese using Conventional Commits and the existing repository style.

## Coding Conventions

- Put player-facing in-game text (commands, interactions, menus, etc.) in `Messages.kt`, not at usage sites. Search the codebase for existing `Messages.kt` files and follow their patterns.
- Never use functions to obtain messages from `Messages.kt`; always define messages as values and inject dynamic content through placeholders, even if existing code uses function-based messages.
