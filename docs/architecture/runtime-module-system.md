# Runtime Module System Refactor

## Status

- Status: Approved for implementation
- Branch: `refactor/runtime-module-system`
- Scope: Gradle conventions, module layout, runtime module lifecycle, legacy module migration

## Goals

- Replace the current Gradle conventions with small, orthogonal conventions.
- Replace `FeatureManagerImplV2` with a unified runtime module manager.
- Give the Kernel a fixed and explicit startup path.
- Manage features and capabilities through the same lifecycle and dependency graph.
- Start capabilities only when required, then keep them running until server shutdown.
- Load and enable modules only during the platform startup lifecycle.
- Support disabling individual features at runtime as a one-way emergency operation.
- Migrate all flat `framework-*` and `feature-*` projects into the new directory layout.
- Preserve the existing DDD rules used by features under `feature/`.
- Avoid forcing simple legacy features into empty DDD layers.
- Preserve one unified distribution JAR containing both Paper and Velocity platforms.

## Non-Goals

- Producing a different distribution artifact for each feature selection.
- Splitting the current unified Paper and Velocity distribution into platform-specific JARs.
- Providing a separate classloader for each feature.
- Persisting runtime management commands back into configuration.
- Loading, enabling, or retrying modules after platform startup.
- Providing Kernel-level feature reload; feature-specific configuration refresh remains a business concern.
- Guaranteeing complete cleanup of side effects created by feature or capability code.
- Rewriting all legacy business logic during the structural migration.
- Adding unit tests to legacy feature implementations.
- Upgrading Gradle, Kotlin, or third-party dependencies as part of this refactor.

## Terminology

### Gradle Project

A physical build project such as `:feature:home:paper`. A Gradle project does not necessarily have a runtime lifecycle.

### Runtime Module

A discoverable object managed by the Kernel. Runtime modules have descriptors, dependencies, state, and lifecycle hooks.

### Feature

A player-facing or administrator-facing behavior selected as a startup root through configuration. An enabled feature may be disabled at runtime, but it cannot be selected or started after platform startup.

### Capability

A reusable, non-gameplay runtime service that owns resources, listeners, jobs, or configuration. Capabilities are activated only through module dependencies.

### Foundation

Stateless, lifecycle-free code shared by multiple modules. Foundation projects are ordinary Gradle libraries and do not participate in runtime discovery.

## Target Project Layout

```text
foundation/
  common/
  paper/
  velocity/

kernel/
  api/
    paper/
    velocity/
  common/
  paper/
  velocity/

capability/
  mongo/
    api/
    common/
    paper/
    velocity/
  charonflow/
    api/
    common/
    paper/
    velocity/
  geoip/
    api/
    common/
    paper/
    velocity/
  database-persist/
    api/
    common/
    paper/
    velocity/
  profile/
    api/
    common/
    velocity/
  interactive/
    api/
    paper/
  server-statistics/
    api/
    paper/
  world-alias/
    api/
      paper/
    paper/

feature/
  <id>/
    core/
    api/
      paper/
      velocity/
    mongo/
    messaging/
    common/
    paper/
    velocity/

platform/
  paper/
  velocity/

build-support/
  module-processor/

catalog/
```

Only projects that contain real code are created. Ancillary build or publication projects such as `catalog`, and feature-owned projects such as `frontend`, are outside the runtime module hierarchy but remain part of the build.

The root build project is the single distribution project. It applies `plutoproject.distribution`, aggregates `platform/paper` and `platform/velocity`, and produces one JAR containing both platform entrypoints, dependency manifests, and runtime module descriptors. The two platform projects are composition roots inside that unified artifact; they are not separate published distributions.

A simple platform-specific feature may consist of one project:

```text
feature/hat/paper
feature/motd/velocity
```

A feature with a Paper-facing API may use:

```text
feature/home/
  api/
    paper/
  paper/
```

A DDD feature may use:

```text
feature/gallery/
  core/
  api/
  mongo/
  common/
  paper/
  frontend/
```

## Project Responsibilities

| Project name | Responsibility |
| --- | --- |
| `core` | Pure domain models and use cases without platform or infrastructure dependencies |
| `api` | Platform-independent outward-facing contracts |
| `api/paper` | Paper-specific extensions of the parent API |
| `api/velocity` | Velocity-specific extensions of the parent API |
| `mongo` | MongoDB documents, mappers, and repository implementations |
| `messaging` | Message types and messaging port implementations |
| `common` | Cross-platform runtime composition and shared adapter behavior |
| `paper` | Paper entrypoint, listeners, commands, and platform interaction |
| `velocity` | Velocity entrypoint, listeners, commands, and platform interaction |
| `frontend` | Optional feature-owned web or UI build that does not participate in runtime discovery |

The `adapter-*`, `infra-*`, and `api-*` naming prefixes are removed. Platform extension APIs use nested projects such as `api/paper`.

## Dependency Direction

Runtime dependency rules:

```text
FEATURE    -> FEATURE | CAPABILITY
CAPABILITY -> CAPABILITY
CAPABILITY -X-> FEATURE
```

Gradle dependency rules:

| Source | Allowed dependencies |
| --- | --- |
| Foundation common | Foundation and platform-independent libraries |
| Foundation paper | Foundation common, other Foundation projects, and the Paper API |
| Foundation velocity | Foundation common, other Foundation projects, and the Velocity API |
| Kernel API | Foundation and platform-independent coroutine and logging contracts exposed by `ModuleContext` |
| Kernel common | Kernel API and Foundation |
| Kernel `api/paper` | Kernel API and `foundation/paper` |
| Kernel `api/velocity` | Kernel API and `foundation/velocity` |
| Kernel paper | Kernel common, Kernel Paper API, and `foundation/paper` |
| Kernel velocity | Kernel common, Kernel Velocity API, and `foundation/velocity` |
| Capability API | Foundation and Kernel API contracts where required |
| Capability `api/paper` | Its parent API when present, Kernel Paper API contracts where required, `foundation/paper`, and the Paper API |
| Capability `api/velocity` | Its parent API when present, Kernel Velocity API contracts where required, `foundation/velocity`, and the Velocity API |
| Capability common | Its own API, Kernel API, Foundation, implementation libraries, and other capability APIs |
| Capability paper | Its own API/common projects, Kernel Paper API, `foundation/paper`, and other capability APIs |
| Capability velocity | Its own API/common projects, Kernel Velocity API, `foundation/velocity`, and other capability APIs |
| Feature core | Kotlin-only dependencies and its own domain ports |
| Feature API | Platform-independent libraries only |
| Feature `api/paper` | Parent API and `foundation/paper` |
| Feature `api/velocity` | Parent API and `foundation/velocity` |
| Feature `mongo` | Its own core, repository ports, MongoDB libraries, and required capability contracts |
| Feature `messaging` | Its own core messaging ports and messaging libraries |
| Feature `common` | Its own core, API, mongo, and messaging projects; Kernel API when it is a runtime entrypoint; plus cross-platform capability APIs |
| Feature `paper` | Its own core/common/API projects, `api/paper`, Kernel Paper API, Foundation, and capability APIs |
| Feature `velocity` | Its own core/common/API projects, `api/velocity`, Kernel Velocity API, Foundation, and capability APIs |
| Feature `frontend` | Feature-owned frontend inputs and generated assets only; no runtime project dependencies |

Foundation must not depend on Kernel, capabilities, or features. A capability must never depend on a feature.

## Runtime Metadata

Feature declarations use explicit dependency names:

```kotlin
@Feature(
    id = "home",
    platform = Platform.PAPER,
    requiredFeatures = ["teleport"],
    optionalFeatures = ["menu"],
    requiredCapabilities = ["mongo", "interactive"],
)
```

Capability declarations use the same runtime model but may only require capabilities:

```kotlin
@Capability(
    id = "database-persist",
    platform = Platform.PAPER,
    requiredCapabilities = ["mongo"],
)
```

Both annotations generate a unified descriptor:

```kotlin
data class ModuleDescriptor(
    val schemaVersion: Int,
    val id: String,
    val type: ModuleType,
    val platform: Platform,
    val entrypoint: String,
    val requiredFeatures: List<String> = emptyList(),
    val optionalFeatures: List<String> = emptyList(),
    val requiredCapabilities: List<String> = emptyList(),
)
```

The processor and descriptor validator enforce:

- Entrypoints implement `RuntimeModule`.
- Entrypoints are public, non-abstract, non-inner classes with a public zero-argument constructor.
- Kotlin `object` declarations are not valid entrypoints because the Kernel owns one explicit instance per startup.
- The processor validates entrypoint shape and metadata local to one compilation.
- The assembled descriptor validator enforces ID uniqueness within each platform discovery set, dependency types, missing dependencies, and cycles.
- A single runtime entry project only emits descriptors for one platform.

Runtime IDs are lowercase, case-sensitive, and must match `[a-z][a-z0-9_-]*`. IDs are used unchanged for configuration, commands, dependency lookup, and manifest filenames. Directory slugs do not define or normalize runtime IDs.

`Load.BEFORE` and `Load.AFTER` are removed. Within each lifecycle phase, dependencies always run before their dependents.

Runtime descriptors use only `Platform.PAPER` or `Platform.VELOCITY`. `common` is a Gradle project role for shared implementation code, not a runtime platform. A module used on both platforms has a thin runtime entry project for each platform; each entry project emits its own descriptor and composes any shared logic from `common`.

Paper and Velocity Kernels are process-local and independent. Each Kernel discovers only descriptors for its current platform. The same ID may therefore have separate PAPER and VELOCITY descriptors, but the Kernel does not infer that they form one distributed module and does not coordinate their state or lifecycle. Cross-platform collaboration belongs to the module's own messaging or business protocol.

Feature and capability IDs share one global namespace within each platform discovery set.

Generated manifests live under:

```text
META-INF/plutoproject/modules/paper/
META-INF/plutoproject/modules/velocity/
```

Each runtime entry project emits one UTF-8 JSON object with `schemaVersion = 1` at one of:

```text
META-INF/plutoproject/modules/<platform>/<id>.json
```

The descriptor's `type` field distinguishes features from capabilities; it is not part of the resource filename because feature and capability IDs share one namespace within a platform. `ModuleDiscovery` reports unknown schema versions and malformed files while retaining source paths. `ModuleDescriptorValidator` rejects duplicate IDs within the current platform discovery set. The same ID may have separate PAPER and VELOCITY descriptors because each Kernel sees only its own platform set. The distribution build rejects duplicate descriptor resource paths before packaging. Any descriptor or static graph validation error aborts platform plugin startup before lifecycle execution and is reported through logs. The KSP processor is moved out of the framework implementation and into `build-support/module-processor`.

## Runtime Module Lifecycle

```kotlin
interface RuntimeModule {
    suspend fun onLoad(context: ModuleContext) {}
    suspend fun onEnable(context: ModuleContext) {}
    suspend fun onDisable(context: ModuleContext) {}
}
```

The Kernel, rather than a module base class, owns:

- Module state transitions.
- Logger creation.
- Data directory resolution.
- Coroutine scope creation and cancellation.
- Instance creation and destruction.
- Cleanup of resources created by the Kernel itself after lifecycle failures.

Modules do not update their own state. Convenience base classes may be provided, but Kernel state correctness must not depend on subclasses invoking Kernel methods. Cleanup of module-created side effects remains the module's responsibility.

The manager creates entrypoints through their validated public zero-argument constructors. Reflection is isolated inside `RuntimeModuleFactory`; feature and capability code does not instantiate other runtime modules.

Stable module states:

```kotlin
enum class ModuleState {
    DISCOVERED,
    LOADED,
    ENABLED,
    DISABLED,
    BLOCKED,
    FAILED,
}
```

The currently running operation is recorded separately:

```kotlin
enum class ModuleOperation {
    LOAD,
    ENABLE,
    DISABLE,
}
```

Lifecycle operations are serialized by the manager. `LOAD` and `ENABLE` occur only during platform startup. Runtime commands may only disable enabled features, and two disable commands must not mutate the active graph concurrently. Lifecycle hooks have no Kernel-enforced timeout; implementations are responsible for returning.

`LOADED` is stable only across the boundary between the platform load and enable stages; it is not a runtime recovery state. `BLOCKED` has no live instance and indicates that the module could not complete startup because a required dependency failed.

## Kernel Startup

The top-level configuration is reduced to one feature root list:

```hocon
enable-features = [
  "home",
  "gallery"
]
```

There is no separate `enabled` and `autoLoad` distinction.

Startup follows a fixed path:

1. Initialize the Kernel.
2. Discover descriptors for the current platform.
3. Validate the complete static dependency graph.
4. Treat `enable-features` as feature roots.
5. Calculate required feature and capability closures.
6. During the platform load phase, run capability `onLoad` hooks and then feature `onLoad` hooks in their respective topological orders.
7. During the platform enable phase, run capability `onEnable` hooks and then feature `onEnable` hooks in their respective topological orders.
8. Report activated modules, blocked modules, and complete failure paths.

A capability does not read its own configuration or allocate resources until it is part of an activation plan.

Capability-to-capability and feature-to-feature edges use separate load and enable phases. Dependencies run before their dependents within each phase. For feature A -> feature B, the ordering is:

```text
B.onLoad
A.onLoad
B.onEnable
A.onEnable
```

`onLoad` runs at the platform's own load stage. On Paper this is plugin `onLoad`; on Velocity this is plugin construction after the platform environment is initialized. `onEnable` runs during Paper plugin `onEnable` or the Velocity proxy initialization event. A feature may use services prepared by a required feature after that dependency's `onLoad` has completed; it does not need to wait for the dependency to become `ENABLED`.

The activation plan is immutable for the process lifetime. Modules outside the configured root closures remain `DISCOVERED`; the Kernel does not activate them later. Unknown feature roots, capability IDs used as roots, and IDs unavailable on the current platform are skipped with warnings while valid roots continue. A descriptor or static graph validation error instead aborts the platform plugin startup.

Two-phase failure handling is deterministic:

1. The load phase continues for independent nodes and marks nodes whose required dependency failed to load as `BLOCKED` without creating their instances.
2. A dependency that loaded successfully remains eligible for enable even when one of its dependents fails to load.
3. The enable phase runs in topological order and enables every successfully loaded node whose required dependencies are enabled.
4. If a required dependency fails to enable, a dependent that already completed `onLoad` receives a best-effort `onDisable`, then the Kernel cancels its module scope, discards its instance, and marks it `BLOCKED`.
5. If an optional dependency fails, the proposed optional edge is discarded and does not block the consumer from enabling.
6. A proposed optional edge becomes active only after both feature instances reach `ENABLED`.

Therefore, if A fails after B loaded successfully, B still proceeds to `onEnable` and remains enabled. If B fails during `onEnable`, a preloaded A receives `onDisable` and becomes `BLOCKED` without a live instance. Lifecycle failures are isolated to the failed module and its dependency descendants; independent modules continue and the platform plugin remains running.

## Active Optional Dependencies

Optional dependencies are captured once from the startup activation plan for the lifetime of a feature instance.

If A declares B as optional, the planner creates a proposed active edge when B is included as a configured root or required dependency in the same immutable startup activation plan:

```text
A -> B
```

That edge blocks B from being disabled until A is disabled. If B is not part of the startup plan, no active edge is created. Proposed active optional edges are included in cycle validation for that activation plan; if they form a cycle, platform plugin startup is aborted before any lifecycle hook executes. Inactive optional metadata does not participate in static cycle validation. A proposed edge becomes active only after both feature instances reach `ENABLED` and is never recalculated at runtime.

## Dynamic Feature Disable

For a graph:

```text
A -> B -> C
```

A depends on B, and B depends on C. The only valid interactive disable order is:

```text
A
B
C
```

Interactive disable rules:

- Disable never cascades to dependencies or dependents.
- Required incoming edges from enabled features block disable.
- Active optional incoming edges also block disable.
- The user must disable dependents manually, from the outermost node inward.
- Disabling a feature does not disable feature dependencies.
- Disabling a feature never disables capabilities.
- A successful disable is permanent for the current process. Re-enabling the feature requires a server restart.
- After `onDisable`, the Kernel cancels and joins the module coroutine scope, destroys the feature instance and its `ModuleContext`, and records `DISABLED` even if the hook failed.
- The Kernel does not guarantee that feature-created side effects were removed.

A rejected operation reports direct blockers and dependency paths, for example:

```text
Cannot disable B because enabled modules still reference it:

A -> B
```

The Kernel has no load, enable, or reload operation after platform startup. A feature that needs runtime configuration refresh provides its own business command and owns that operation's consistency and failure handling.

## Capability Lifecycle

Capabilities use the same stable states and lifecycle hooks as features:

```text
DISCOVERED --onLoad--> LOADED --onEnable--> ENABLED
     |                    |                     |
     +--------------------+---------------------+--> FAILED

DISCOVERED or LOADED --required dependency failure--> BLOCKED
```

Capability activation runs `onLoad` during platform load and `onEnable` during platform enable. Capability `onDisable` runs only during server shutdown. If a capability hook fails, the Kernel only releases resources that it created; capability-created side effects remain the capability's responsibility.

Rules:

- A capability cannot be disabled interactively.
- Once enabled, it remains enabled until server shutdown.
- Disabling every consuming feature does not stop it.
- A failed or blocked capability cannot be retried without restarting the server.

## Lifecycle Failure Outcomes

| Failure | Cleanup and final state |
| --- | --- |
| Descriptor or static dependency validation | Abort platform plugin startup before any instance is created |
| Dependency failure before dependent load | Do not create the dependent instance; set `BLOCKED` and retain the dependency path |
| Dependency enable failure after dependent load | Invoke dependent `onDisable` best-effort, cancel and join its Kernel-created scope, discard the instance, and set `BLOCKED` |
| `onLoad` | Do not invoke `onDisable`; cancel and join the Kernel-created scope, discard the instance, and set `FAILED` |
| `onEnable` | Invoke best-effort `onDisable`, cancel and join the Kernel-created scope, discard the instance, and set `FAILED` |
| `onDisable` | Cancel and join the Kernel-created scope, discard the instance, end in `DISABLED`, and return a failed operation result |

Successfully activated dependencies remain enabled when a dependent feature fails.

## Server Shutdown

Server shutdown does not use the interactive blocker policy.

Shutdown order:

1. Disable active features in reverse active-graph order.
2. Cancel feature scopes and destroy feature contexts and instances.
3. Disable enabled capabilities in reverse capability order.
4. Close platform Kernel resources.
5. Cancel the Kernel root scope and close Kernel-owned infrastructure.

An `onDisable` exception is reported but must not prevent cleanup of Kernel-created resources or the shutdown of other modules.

## Module Resource Responsibility

The Kernel does not track or clean resources created by feature or capability code.

```kotlin
interface ModuleContext {
    val id: String
    val logger: Logger
    val dataFolder: Path
    val coroutineScope: CoroutineScope
}
```

The Kernel creates the module coroutine scope and therefore always cancels and joins it when the module ends. The Kernel also clears its own state and instance references. Listeners, commands, scheduler tasks, recipes, GUI state, subscriptions, service hooks, Koin definitions, database clients, and all other side effects created by module code remain the module's responsibility.

Feature and capability implementations should clean their side effects in `onDisable`, but the Kernel does not enforce or verify complete cleanup. Existing features may be structurally migrated without adding missing cleanup behavior. A module may therefore leave registrations or other effects behind after reaching `DISABLED`; conflicts and residue are defects or limitations of that module rather than Kernel state inconsistencies.

Features access Koin directly for now and are responsible for loading and unloading their definitions. DI isolation is deferred to a separate design.

The disable cleanup sequence is:

1. Invoke feature `onDisable` best-effort.
2. Cancel and join the Kernel-created feature coroutine scope even if the hook failed.
3. Remove the feature context and instance from the Kernel.
4. Store and report the complete operation result.

## Cloud Commands

The fixed `/plutoproject` command belongs to the Kernel and remains available independently of feature state. Feature commands are registered and removed by their owning features. Features should use Cloud's `deleteRootCommand` from `onDisable` when they support command cleanup, but the Kernel neither tracks command ownership nor guarantees that disabled feature commands disappear. Command root conflicts and global parsers retaining feature instances are feature-level concerns.

## Runtime Manager Components

`FeatureManagerImplV2` is replaced rather than extended. The new implementation is split into:

```text
ModuleDiscovery
ModuleDescriptorValidator
ModuleGraph
ModulePlanner
RuntimeModuleFactory
RuntimeModuleManager
ModuleRegistry
ModuleOperationReporter
```

### ModuleDiscovery

Loads manifests for the current platform, preserves descriptor source information, and reports malformed resources or unsupported schemas.

### ModuleDescriptorValidator

Validates platform compatibility, ID uniqueness within the current platform discovery set, dependency types, missing dependencies, and illegal capability-to-feature edges.

### ModuleGraph

A pure Kotlin component responsible for:

- Required dependency edges.
- Optional feature metadata.
- Active optional edges.
- Reverse dependency lookups.
- Required dependency closure calculation.
- Topological activation order.
- Reverse shutdown order.
- Static required-edge cycle detection and per-plan active optional-edge cycle detection.
- Disable blocker paths.

### ModulePlanner

Creates one immutable startup plan before lifecycle execution:

```kotlin
data class ActivationPlan(
    val capabilities: List<ModuleDescriptor>,
    val features: List<ModuleDescriptor>,
)
```

### RuntimeModuleManager

Executes the startup plan, serializes runtime disable operations, owns states and contexts, and applies failure propagation.

### ModuleRegistry

Provides state and descriptor queries without exposing feature instances or private dependency injection containers.

## Public Management APIs

The complete runtime manager is internal to the Kernel.

Features receive a read-only registry:

```kotlin
interface FeatureRegistry {
    fun state(id: String): ModuleState?
    fun isEnabled(id: String): Boolean
}
```

Administrative commands use dedicated controllers:

```kotlin
interface FeatureController {
    suspend fun disable(id: String): ModuleOperationResult
}
```

Operation outcomes use explicit result types rather than Boolean values:

```kotlin
sealed interface ModuleOperationResult {
    data class Success(/* ... */) : ModuleOperationResult
    data class Rejected(/* blockers and reasons */) : ModuleOperationResult
    data class Failed(/* phase and cause */) : ModuleOperationResult
}
```

The following service-locator patterns are removed:

```kotlin
FeatureManager.getFeature(...)
FeatureManager.isEnabled(...)
MongoConnection.getCollection(...)
```

Cross-feature integrations depend on the provider's API project and obtain services through normal dependency injection. Features access Koin directly until DI isolation receives a separate design.

## Administrative Commands

```text
/plutoproject feature list
/plutoproject feature info <id>
/plutoproject feature disable <id>

/plutoproject capability list
/plutoproject capability info <id>

/plutoproject module graph <id>
```

The information command reports:

- Module type and platform.
- Current stable state and running operation.
- Required feature dependencies.
- Active optional feature dependencies.
- Required capabilities.
- Enabled direct dependents.
- Latest operation result and failure summary.

Commands affect only the current process and never modify `enable-features`.

Suggested permissions:

```text
plutoproject.command.feature.list
plutoproject.command.feature.info
plutoproject.command.feature.disable
plutoproject.command.capability.list
plutoproject.command.capability.info
plutoproject.command.module.graph
```

Player-facing command messages are defined in `Message.kt` in the relevant Kernel platform package.

## Gradle Convention Specification

The new conventions are:

```text
plutoproject.kotlin-library
plutoproject.kotlin-test
plutoproject.paper
plutoproject.velocity
plutoproject.runtime-module
plutoproject.distribution
```

Responsibilities:

| Convention | Responsibility |
| --- | --- |
| `kotlin-library` | Java/Kotlin toolchain and common compiler settings |
| `kotlin-test` | JUnit, coroutine test, and shared test task configuration |
| `paper` | Paper API and Paper compilation settings |
| `velocity` | Velocity API and Velocity compilation settings |
| `runtime-module` | KSP processor wiring and manifest project identity |
| `distribution` | Unified Paper and Velocity Shadow JAR, build manifest, and both platform runtime dependency lists |

Conventions configure build behavior. They do not add MongoDB, Koin, Ktor, CharonFlow, LuckPerms, or other business dependencies implicitly.

Additional rules:

- Repositories are centralized in `settings.gradle.kts`.
- KSP, Kapt, Compose, and serialization are applied only where used.
- Every project declares its direct dependencies explicitly.
- Gradle coordinates are derived deterministically from the complete project path.
- `:feature:home:paper` uses a coordinate equivalent to `club.plutoproject.feature.home:paper`.
- The root distribution project owns both platform runtime dependency manifests and packages both platform composition roots into one JAR.
- Project names are never inspected with `contains("paper")`, `contains("feature")`, or similar environment heuristics.

The final migration removes:

```text
plutoproject.legacy-base-conventions
plutoproject.common-conventions
plutoproject.adapter-paper-conventions
plutoproject.adapter-velocity-conventions
plutoproject.platform-conventions
PlutoDependencyHandlerExtension
ProjectExts module-type inference
```

## Actionable Implementation Plan

Intermediate migration phases and commits are not required to leave the complete repository buildable or preserve all gameplay behavior. Targeted modules and tests should still be verified when useful, but only the final acceptance requires the full build, complete test suite, and gameplay verification to pass. New and old conventions may coexist temporarily during migration, but no compatibility layer remains in the final architecture.

### Phase 0: Baseline and Documentation

Tasks:

- Work on `refactor/runtime-module-system`.
- Commit this specification.
- Run the current `./gradlew test`.
- Run the current `./gradlew shadowJar`.
- Record pre-existing failures separately from refactor failures.
- Capture the current feature manifest and dependency graph for migration comparison.

Acceptance criteria:

- The baseline build result is known.
- This specification is version-controlled.
- No gameplay behavior has changed.

Suggested commit:

```text
docs: 添加运行时模块系统重构规范
```

### Phase 1: Replace Gradle Conventions

Tasks:

- Add the new orthogonal conventions under `build-logic/src/main/kotlin`.
- Centralize dependency repositories in `settings.gradle.kts`.
- Remove global dependency forcing from the new base convention.
- Stop injecting all third-party dependencies into every project.
- Establish deterministic groups from complete project paths.
- Keep new and old conventions side by side only while projects are being migrated.
- Add the base architecture verification task for known project path patterns and dependency directions.
- Defer `plutoproject.runtime-module` processor wiring until Phase 2 creates the processor.

Acceptance criteria:

- Projects migrated in this phase use the new base convention and declare their dependencies explicitly; temporary failures in projects not yet migrated do not block the phase.
- Each project receives only explicitly declared dependencies.
- KSP, serialization, and platform plugins are no longer inherited by unrelated projects.

Suggested commit:

```text
build: 重构 Gradle conventions
```

### Phase 2: Introduce Kernel API and Module Processor

Tasks:

- Create `kernel/api`.
- Create `kernel/api/paper` and `kernel/api/velocity`.
- Define `RuntimeModule`, module contexts, descriptors, states, and operation results.
- Define `@Feature` and `@Capability`.
- Create `build-support/module-processor`.
- Add `plutoproject.runtime-module` after the processor project exists.
- Generate Paper and Velocity manifests.
- Add processor tests for valid and invalid declarations.
- Stop generating `Load.BEFORE` and `Load.AFTER` metadata.

Acceptance criteria:

- Feature and capability entrypoints generate unified descriptors.
- Invalid local annotation shapes and entrypoint types are rejected.
- The processor does not depend on a runtime implementation module.

Suggested commit:

```text
feat(kernel): 定义统一运行时模块协议
```

### Phase 3: Implement Module Graph and Runtime Manager

Tasks:

- Create `kernel/common`.
- Implement discovery, descriptor validation, graph, planner, registry, factory, manager, and reporting.
- Calculate activation plans before invoking lifecycle code.
- Implement required feature and capability closures.
- Implement active optional dependency edges.
- Implement reverse dependency blockers and path reporting.
- Implement sticky enabled capability state.
- Implement startup-only load and enable phases.
- Implement permanent runtime feature disable.
- Implement `BLOCKED` state and dependency failure paths.
- Serialize lifecycle operations.
- Validate duplicate IDs within each platform discovery set, missing dependencies, platform compatibility, illegal dependency types, and required-edge cycles.

Required tests:

- A -> B -> C runs `C.load, B.load, A.load, C.enable, B.enable, A.enable`.
- A dependent preloaded before a required dependency enable failure receives `onDisable`, has its Kernel-created scope cancelled, and enters `BLOCKED`.
- A, B, C can only be disabled in A, B, C order.
- Disabling B while A is enabled returns the A -> B blocker path.
- An optional B in the startup plan creates an active edge after both modules enable.
- An optional B outside the startup plan is not loaded.
- A startup plan whose proposed optional edges form a cycle is rejected before lifecycle execution.
- If A fails after starting B, B remains enabled.
- Capability disable is unavailable during runtime.
- A capability remains enabled after every consumer is disabled.
- A failed capability cannot be retried at runtime.
- A disabled feature cannot be loaded or enabled again at runtime.
- The same ID in separate PAPER and VELOCITY descriptors is accepted, while a duplicate within one platform set is rejected.
- Descriptor or static graph validation failure aborts platform plugin startup.
- Independent modules continue when a lifecycle hook fails.

Acceptance criteria:

- The graph and manager are platform-independent and unit tested.
- No manager operation recursively calls another public lifecycle operation.
- Operation results retain phase, cause, and dependency path details.

Suggested commit:

```text
feat(kernel): 实现运行时模块管理器
```

### Phase 4: Implement Platform Kernel Bootstrap

Tasks:

- Create `kernel/paper` and `kernel/velocity`.
- Wire the new Kernel into Paper and Velocity platform lifecycle entrypoints.
- Run module `onLoad` from Paper plugin `onLoad` and Velocity plugin construction.
- Run module `onEnable` from Paper plugin `onEnable` and the Velocity proxy initialization event.
- Keep the old FeatureManager temporarily for unmigrated legacy features, but route newly migrated runtime modules only through the new Kernel.
- Create the minimal platform module contexts required to expose platform APIs without resource ownership wrappers.
- Create and cancel one Kernel-owned coroutine scope per module.
- Keep module-created listeners, commands, tasks, recipes, subscriptions, GUI state, hooks, and Koin definitions outside Kernel ownership.
- Guarantee cleanup of Kernel-created resources after `onDisable` failure.

Acceptance criteria:

- Module jobs launched in the Kernel-created coroutine scope are cancelled and joined.
- Feature-created side effects are not required to disappear after disable.
- A disabled feature cannot be started again without restarting the server.
- One `onDisable` failure does not prevent Kernel scope cleanup or shutdown of other modules.
- A test runtime module can be discovered and managed through the real Paper and Velocity Kernel bootstrap.

Suggested commit:

```text
feat(kernel): 实现平台模块启动生命周期
```

### Phase 5: Add Administrative Commands

Tasks:

- Register the fixed `/plutoproject` command in each platform Kernel.
- Implement feature list, info, and disable.
- Implement capability list and info.
- Implement module graph inspection.
- Add command permissions.
- Add command messages to platform `Message.kt` files.
- Display dependency blockers and the latest operation error.

Acceptance criteria:

- Commands do not modify configuration files.
- Concurrent lifecycle commands are serialized.
- Disable rejection reports accurate dependency paths.
- The Kernel command remains available regardless of feature state.

Suggested commit:

```text
feat(kernel): 添加运行时模块管理命令
```

### Phase 6: Extract Capabilities

Extract capabilities in dependency order:

| Order | Capability | Current source |
| --- | --- | --- |
| 1 | `mongo` | `framework-common/connection` |
| 2 | `charonflow` | `framework-common/connection` |
| 3 | `geoip` | `framework-common/connection` |
| 4 | `database-persist` | `framework-common/databasepersist` |
| 5 | `profile` | `framework-common/profile` and the Velocity profile listener |
| 6 | `interactive` | `framework-paper/interactive` |
| 7 | `server-statistics` | `framework-paper/statistic` and its API contracts |
| 8 | `world-alias` | `framework-paper/worldalias`, its API contracts, and configuration |

Classification decisions:

- The platform command manager belongs to the Kernel because `/plutoproject` always requires it.
- Server statistics is the Paper `server-statistics` capability. Its consumers are `status`, `dynamic-scheduler`, and `overload-warning`.
- WorldAlias is the Paper `world-alias` capability. Its consumers are `teleport`, `home`, and `warp`.
- Toast and stateless Paper DSL code move to `foundation/paper`.
- BuildInfo belongs to the Kernel.
- Stateless configuration loading code moves to `foundation/common`.

Tasks for each capability:

- Create only the required API, common, Paper, and Velocity projects.
- Move contracts out of the old framework API.
- Move implementations and configuration into the capability.
- Add `@Capability` to the runtime entrypoint.
- Add thin Paper and Velocity runtime entry projects when a capability uses the same common implementation on both platforms.
- Declare transitive capability dependencies.
- Replace global companion service lookup with dependency injection.
- Keep capability-created resources under the capability's own lifecycle responsibility.
- Add new-structure unit tests where applicable.
- Remove the corresponding unconditional legacy initialization path as soon as the replacement capability is wired.

Acceptance criteria:

- `enable-features = []` does not read external connection configuration.
- MongoDB is not initialized without a consumer.
- CharonFlow is not initialized without a consumer.
- Interactive listeners are not registered without a consumer.
- Server statistics is not initialized without a consumer.
- World alias configuration is not read without a consumer.
- Successfully enabled capabilities receive `onDisable` only during server shutdown.
- Failed capabilities have only their Kernel-created resources reclaimed; capability code owns any partial side effects.
- Capability failures identify all blocked features.

Capabilities should be committed separately to keep reviews and regressions focused.

### Phase 7: Rename Existing DDD Feature Projects

Apply the simplified naming scheme:

```text
adapter-common   -> common
adapter-paper    -> paper
adapter-velocity -> velocity
infra-mongo      -> mongo
infra-messaging  -> messaging
api-paper        -> api/paper
api-velocity     -> api/velocity
```

Initial targets:

- `feature/whitelist-v2`
- `feature/gallery`

Tasks:

- Rename Gradle project paths and directories.
- Update project dependencies and conventions.
- Preserve package names unless changing them materially improves consistency.
- Declare `requiredCapabilities` explicitly.
- Replace global MongoConnection access; features may continue to access Koin directly until DI isolation is designed separately.
- Record the existing core, API, and gameplay behavior that must be preserved or restored by final acceptance.

Migration checkpoint:

- Existing unit tests remain available as final-regression coverage; run relevant subsets during migration when useful.
- Paper and Velocity descriptors are generated correctly.
- Track any temporary Gallery frontend assembly breakage that must be restored before final acceptance.

Suggested commit:

```text
refactor(feature): 简化新版功能模块命名
```

### Phase 8: Migrate Legacy Features

Move one feature at a time and remove it from the old aggregate immediately after cutover. Do not package old and new entrypoints with the same ID.

Names in the migration waves are project-directory slugs, not runtime IDs. Runtime IDs remain unchanged unless a separate compatibility migration is explicitly approved. For example, `no-player-cap` retains `no_player_cap`, `server-selector` retains `server_selector`, `itemframe-protection` retains `itemframe_protection`, and `whitelist-v2` retains `whitelist_v2`.

#### Wave 1: Simple Paper Features

```text
gm
align
hat
head
suicide
creeper-firework
farm-protection
no-creeper-block-breaks
no-join-quit-message
no-player-cap
recipe
recipe-unlock
dev-watermark
lectern-protection
```

#### Wave 2: Simple Velocity Features

```text
motd
player-cap
join-quit-message
version-checker
```

#### Wave 3: API Providers and Dependency Roots

```text
afk
menu
teleport
elevator
```

#### Wave 4: Features Depending on Shared Feature APIs

```text
back
home
warp
random-teleport
status
```

#### Wave 5: Data and Complex Lifecycle Features

```text
daily
exchange-shop
dynamic-scheduler
pvp-toggle
sit
overload-warning
```

#### Wave 6: Cross-Platform and Integration Features

```text
server-selector
itemframe-protection
```

Migration rules:

- A simple feature receives only a `paper` or `velocity` project.
- Existing Paper-facing contracts move to `feature/<id>/api/paper`.
- Platform-independent contracts move to `feature/<id>/api`.
- Server selector shared code moves to its `common` project.
- Direct MongoDB code may remain in a platform project during structural migration, but the feature must declare the `mongo` capability.
- Legacy features are not forced into DDD layers.
- The legacy Velocity whitelist is removed in favor of whitelist-v2.
- No unit tests are added to legacy feature implementations.
- Existing features do not need new `onDisable` resource cleanup to qualify as structurally migrated.

Migration tracking for every wave:

- Record runtime ID and dependency changes so final compatibility can be verified.
- Record any temporarily broken cross-system integration or gameplay behavior that must be restored before final acceptance.

### Phase 9: Cut Over Platform Bootstrap

Tasks:

- Move Paper and Velocity composition roots to `platform/paper` and `platform/velocity`.
- Remove the temporary legacy platform path after all runtime modules have moved to the fixed Kernel lifecycle.
- Delete `FeatureManagerImplV2`.
- Delete the old FeatureManager API and global companion accessor.
- Delete legacy manifest loading and fallback files.
- Delete flat `framework-*` projects.
- Delete flat `feature-*` projects.
- Rewrite `settings.gradle.kts` to contain only the final module tree.
- Delete every old convention and module-name heuristic.
- Apply `plutoproject.distribution` to the root project and aggregate both `platform/paper` and `platform/velocity` into the unified JAR.

Acceptance criteria:

- No flat legacy module remains.
- No source file calls the old FeatureManager.
- Feature and capability services are not obtained through global companion delegation.
- Build logic does not infer environments from project names.
- The unified distribution contains both platform entrypoints, both runtime dependency manifests, and descriptors for both platforms.

Suggested commit:

```text
refactor: 切换到统一运行时模块系统
```

### Phase 10: Enforce Architecture and Update Documentation

Tasks:

- Enforce project dependency direction in Gradle checks.
- Prevent feature core projects from depending on platform or infrastructure projects.
- Prevent capabilities from depending on features.
- Prevent Foundation from depending on Kernel or runtime modules.
- Require runtime entry projects to apply the runtime module convention.
- Update `AGENTS.md` with the simplified feature naming.
- Add examples for simple, cross-platform, and DDD features.
- Reconcile this specification with the final implementation.

Acceptance criteria:

- Illegal project dependencies fail during `check`.
- New feature authors have an explicit module template.
- Documentation and implementation use the same terminology and paths.

## Automated Verification

Run targeted compilation and tests during intermediate phases when they provide useful feedback. Intermediate full-build or gameplay failures are allowed and must be tracked when they affect final acceptance.

The final refactor must pass both commands from a clean checkout:

```bash
./gradlew test
./gradlew shadowJar
```

## Manual Verification Scenarios

1. Start with `enable-features = []` and no database configuration; verify no capability reads external connection configuration.
2. Configure a feature without database requirements, restart, and verify MongoDB and Redis remain inactive.
3. Configure a MongoDB-dependent feature, restart, and verify Mongo starts during the platform startup lifecycle.
4. Make Mongo initialization fail and verify only dependent modules become `BLOCKED` while independent modules enable.
5. Verify the failed capability and its blocked dependents cannot be retried or enabled without restarting.
6. Configure A where A -> B -> C and verify `C.load, B.load, A.load, C.enable, B.enable, A.enable` across the two platform phases.
7. Attempt to disable B while A is enabled and verify the A -> B blocker path.
8. Disable A, B, and C in that order.
9. Verify capabilities remain enabled after every consumer is disabled.
10. Verify a disabled feature cannot be loaded or enabled again until server restart.
11. Verify disabling a feature cancels and joins its Kernel-created coroutine scope; feature-created registrations are best-effort and may remain.
12. Shut down the server and verify features receive `onDisable` before capabilities.

## Completion Criteria

The refactor is complete when:

- The fixed Kernel is the only unconditional runtime layer.
- Feature and capability lifecycle is managed by one runtime manager.
- Capabilities activate from static requirements and remain active until shutdown.
- Module load and enable occur only during platform startup.
- Runtime feature management is limited to one-way disable; recovery requires server restart.
- The Kernel reclaims its own module scopes and internal state without claiming ownership of module-created side effects.
- All legacy flat projects and old conventions have been removed.
- Existing DDD features use the simplified project names.
- One unified distribution JAR contains the Paper and Velocity platforms, dependency manifests, and runtime descriptors.
- The complete test and Shadow JAR builds pass.
