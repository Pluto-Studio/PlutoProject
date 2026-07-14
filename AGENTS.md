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
- Write Git commit messages in Chinese using Conventional Commits and the existing repository style.

## Coding Conventions

- Put player-facing in-game text (commands, interactions, menus, etc.) in `Messages.kt`, not at usage sites. Search the codebase for existing `Messages.kt` files and follow their patterns.
