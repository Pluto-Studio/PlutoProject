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
- Support loading, disabling, and reloading individual features at runtime.
- Support explicit retry of failed capabilities.
- Migrate all flat `framework-*` and `feature-*` projects into the new directory layout.
- Preserve the existing DDD rules used by features under `feature/`.
- Avoid forcing simple legacy features into empty DDD layers.
- Preserve one unified distribution JAR containing both Paper and Velocity platforms.

## Non-Goals

- Producing a different distribution artifact for each feature selection.
- Splitting the current unified Paper and Velocity distribution into platform-specific JARs.
- Providing a separate classloader for each feature.
- Persisting runtime management commands back into configuration.
- Rewriting all legacy business logic during the structural migration.
- Adding unit tests to legacy feature implementations.
- Upgrading Gradle, Kotlin, or third-party dependencies as part of this refactor.

## Terminology

### Gradle Project

A physical build project such as `:feature:home:paper`. A Gradle project does not necessarily have a runtime lifecycle.

### Runtime Module

A discoverable object managed by the Kernel. Runtime modules have descriptors, dependencies, state, and lifecycle hooks.

### Feature

A player-facing or administrator-facing behavior that may be selected directly through configuration or runtime commands.

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
  charonflow/
    api/
    common/
  geoip/
    api/
    common/
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
| Kernel API | Foundation and the Koin API exposed by `ModuleContext` |
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
- Kotlin `object` declarations are not valid entrypoints because reload after disable requires a fresh instance.
- The processor validates entrypoint shape and metadata local to one compilation.
- The assembled descriptor validator enforces ID uniqueness within each platform discovery set, dependency types, missing dependencies, and cycles.
- A single runtime entry project only emits descriptors for one platform.

Runtime IDs are lowercase, case-sensitive, and must match `[a-z][a-z0-9_-]*`. IDs are used unchanged for configuration, commands, dependency lookup, and manifest filenames. Directory slugs do not define or normalize runtime IDs.

`Load.BEFORE` and `Load.AFTER` are removed. Within each lifecycle phase, dependencies always run before their dependents.

`Platform.COMMON` means that a descriptor is compatible with both platforms. Discovery combines COMMON descriptors with the current platform. Feature and capability IDs share one global namespace within that platform discovery set. A COMMON descriptor and a current-platform descriptor with the same ID are an error; there is no override or merge behavior. Platform-specific implementations therefore emit only PAPER or VELOCITY descriptors, while truly cross-platform runtime implementations emit COMMON descriptors.

Generated manifests live under:

```text
META-INF/plutoproject/modules/common/
META-INF/plutoproject/modules/paper/
META-INF/plutoproject/modules/velocity/
```

Each runtime entry project emits one UTF-8 JSON object with `schemaVersion = 1` at:

```text
META-INF/plutoproject/modules/<platform>/<type>-<id>.json
```

`ModuleDiscovery` reports unknown schema versions and malformed files while retaining source paths. `ModuleDescriptorValidator` rejects duplicate IDs after combining COMMON descriptors with the current platform. The same ID may have separate PAPER and VELOCITY descriptors because those descriptors are never part of the same platform discovery set. The distribution build rejects duplicate descriptor resource paths before packaging. All failures occur before lifecycle execution. The KSP processor is moved out of the framework implementation and into `build-support/module-processor`.

## Runtime Module Lifecycle

```kotlin
interface RuntimeModule {
    suspend fun onLoad(context: ModuleContext) {}
    suspend fun onEnable(context: ModuleContext) {}
    suspend fun onReload(context: ModuleContext) {}
    suspend fun onDisable(context: ModuleContext) {}
}
```

The Kernel, rather than a module base class, owns:

- Module state transitions.
- Logger creation.
- Data directory resolution.
- Coroutine scope creation and cancellation.
- Dependency injection registration and removal.
- Platform resource tracking.
- Instance creation and destruction.
- Cleanup after lifecycle failures.

Modules do not update their own state. Convenience base classes may be provided, but correctness must not depend on subclasses invoking Kernel cleanup methods.

The manager creates entrypoints through their validated public zero-argument constructors. Reflection is isolated inside `RuntimeModuleFactory`; feature and capability code does not instantiate other runtime modules.

Stable module states:

```kotlin
enum class ModuleState {
    DISCOVERED,
    LOADED,
    ENABLED,
    DISABLED,
    FAILED,
}
```

The currently running operation is recorded separately:

```kotlin
enum class ModuleOperation {
    LOAD,
    ENABLE,
    RELOAD,
    DISABLE,
    RETRY,
}
```

Lifecycle operations are serialized by the manager. Two commands must not mutate the active graph concurrently.

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
2. Discover common and current-platform descriptors.
3. Validate the complete static dependency graph.
4. Treat `enable-features` as feature roots.
5. Calculate required feature and capability closures.
6. Run capability `onLoad` hooks in capability topological order.
7. Run capability `onEnable` hooks in capability topological order.
8. Run feature `onLoad` hooks in feature topological order.
9. Run feature `onEnable` hooks in feature topological order.
10. Report activated modules, blocked modules, and complete failure paths.

A capability does not read its own configuration or allocate resources until it is part of an activation plan.

Capabilities are fully enabled before any dependent feature enters `onLoad`. Capability-to-capability edges use capability load and enable phases in topological order. Feature-to-feature edges use a separate two-phase execution. For feature A -> feature B, the ordering is:

```text
B.onLoad
A.onLoad
B.onEnable
A.onEnable
```

`onLoad` prepares module-local configuration, definitions, and resources that do not require feature dependencies to be enabled. Required capabilities are already enabled at this point. Runtime behavior that consumes an enabled feature dependency starts in `onEnable`. Dynamic feature loading follows the same four-stage capability-load, capability-enable, feature-load, feature-enable ordering for the newly activated plan.

Two-phase failure handling is deterministic:

1. The load phase continues for independent nodes and skips nodes whose required dependency failed to load.
2. A dependency that loaded successfully remains eligible for enable even when one of its dependents fails to load.
3. The enable phase runs in topological order and enables every successfully loaded node whose required dependencies are enabled.
4. If a required dependency fails to enable, a dependent that already completed `onLoad` is cleaned without calling its normal `onDisable`, its instance is discarded, and it returns to `DISCOVERED` with a blocked result.
5. If an optional dependency fails, the proposed optional edge is discarded and does not block the consumer from enabling.
6. A proposed optional edge becomes active only after both feature instances reach `ENABLED`.

Therefore, if A fails after B loaded successfully, B still proceeds to `onEnable` and remains enabled. If B fails during `onEnable`, a preloaded A is cleaned and remains blocked without a live instance.

## Dynamic Feature Loading

```text
/plutoproject feature load <id>
```

At runtime, `load` means complete activation. It executes both `onLoad` and `onEnable`.

Loading a feature:

- Automatically activates required feature dependencies.
- Automatically activates required capabilities and their transitive capability dependencies.
- Does not automatically activate optional features.
- Captures an optional edge only when that feature is already enabled or independently included in the same activation plan.
- Creates a fresh feature instance.
- Does not modify `enable-features` or any other persisted configuration.

The manager calculates an activation plan before executing lifecycle hooks. It does not recursively invoke lifecycle methods while traversing dependencies.

If loading A starts required feature B successfully and A later fails, B remains enabled. Successful feature activation is not rolled back implicitly.

If a disabled or failed feature is loaded again, the manager creates a new instance and a new `ModuleContext`.

## Active Optional Dependencies

Optional dependencies are captured for the lifetime of a feature instance.

If A declares B as optional, the planner creates an active edge when B was enabled before the operation or B is included as a configured root or required dependency in the same immutable activation plan:

```text
A -> B
```

That edge blocks B from being disabled until A is disabled. If B is neither already enabled nor part of the same plan, no active edge is created. Loading B later does not modify A and does not invoke an integration callback. Proposed active optional edges are included in cycle validation for that activation plan; inactive optional metadata does not participate in static cycle validation.

Disabling and loading A again recalculates its optional edges.

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
- A successful disable destroys the feature instance and its `ModuleContext`.

A rejected operation reports direct blockers and dependency paths, for example:

```text
Cannot disable B because enabled modules still reference it:

A -> B
```

## Feature Reload

```text
/plutoproject feature reload <id>
```

Reload semantics:

- Reload is available only for an `ENABLED` feature.
- Reload invokes only `onReload`.
- Reload does not invoke disable, load, or enable.
- Reload is not a full re-enable operation.
- Reload does not replace the feature instance.
- Reload does not recalculate required or optional dependencies.
- A reload failure leaves the feature in the `ENABLED` state.
- The latest reload failure is retained for the feature information command.
- The Kernel does not provide a transactional reload scope, automatically replace registrations, or roll back work performed by `onReload`.

Feature implementations are responsible for performing a safe reload inside `onReload`. This includes loading and validating replacement configuration before committing it, safely replacing any module-owned services or registrations that the feature chooses to change, and retaining a viable enabled instance when reload fails.

## Capability Lifecycle

Capabilities use the same stable states and lifecycle hooks as features:

```text
DISCOVERED --onLoad--> LOADED --onEnable--> ENABLED
     |                    |                     |
     +--------------------+---------------------+--> FAILED
```

`STARTING` is an operation status, not a stable state. Capability activation runs `onLoad` and then `onEnable` in dependency order. Capability `onDisable` runs only during server shutdown, except that a partially initialized failed instance is cleaned immediately and discarded.

Rules:

- A capability cannot be disabled interactively.
- A capability does not expose a normal reload operation.
- Once enabled, it remains enabled until server shutdown.
- Disabling every consuming feature does not stop it.
- A failed capability is not retried by later feature load commands.
- Retry is always an explicit administrator action.

```text
/plutoproject capability retry <id>
```

An explicit retry of a target capability authorizes retries across that target's required capability closure. Retry behavior:

1. Calculate the target capability's required capability closure.
2. Activate `FAILED` and `DISCOVERED` dependencies in topological order.
3. Skip already enabled capabilities.
4. Create fresh instances for failed capabilities.
5. Start the target capability.
6. Do not automatically reload features that previously failed.

Partially initialized capability resources must be cleaned up before a retry instance is created.

Retry is accepted only when the selected target capability is `FAILED`. Already enabled targets return a rejected no-op result; normal first activation remains feature-driven.

## Lifecycle Failure Outcomes

| Failure | Cleanup and final state |
| --- | --- |
| Descriptor or dependency validation | No instance is created; module remains `DISCOVERED` with a rejected result |
| Dependency activation before dependent load | Dependent instance is not created; module remains `DISCOVERED` with a blocked result and dependency path |
| Dependency enable after dependent load | Close the dependent context, cancel its scope, unload DI, discard its preloaded instance, and return it to `DISCOVERED` with a blocked result |
| `onLoad` | Close the context, cancel the scope, unload DI, discard the instance, and set `FAILED` |
| `onEnable` | Invoke best-effort `onDisable`, then close context, cancel scope, unload DI, discard the instance, and set `FAILED` |
| `onReload` | Do not perform generic teardown; retain the instance and `ENABLED` state, and record the failed reload result |
| `onDisable` | Continue all Kernel cleanup, discard the instance, end in `DISABLED`, and return a failed operation result |

Successfully activated dependencies remain enabled when a dependent feature fails.

## Server Shutdown

Server shutdown does not use the interactive blocker policy.

Shutdown order:

1. Disable active features in reverse active-graph order.
2. Destroy feature contexts and instances.
3. Disable enabled capabilities in reverse capability order.
4. Close platform Kernel resources.
5. Cancel the Kernel root scope and close dependency injection.

An `onDisable` exception is reported but must not prevent Kernel cleanup or the shutdown of other modules.

## Module Resource Ownership

Reliable runtime disable requires all registrations to be associated with their owning module.

```kotlin
interface ModuleContext {
    val id: String
    val logger: Logger
    val dataFolder: Path
    val coroutineScope: CoroutineScope

    fun own(resource: AutoCloseable)
    fun onClose(action: () -> Unit)
    fun installDi(module: Module)
}
```

The public `ModuleContext` deliberately has no `close()` operation. The Kernel implementation wraps it in an internal `ManagedModuleContext : ModuleContext, AutoCloseable`; only `RuntimeModuleManager` can close that managed context.

Platform contexts extend the common contract:

```kotlin
interface PaperModuleContext : ModuleContext {
    fun registerListener(listener: Listener)
    fun registerCommands(roots: Set<String>, command: Any)
    fun registerRecipe(key: NamespacedKey, recipe: Recipe)
    fun ownTask(task: BukkitTask)
    fun ownGuiScope(scope: AutoCloseable)
}
```

```kotlin
interface VelocityModuleContext : ModuleContext {
    fun registerListener(listener: Any)
    fun registerCommands(roots: Set<String>, command: Any)
    fun ownTask(task: ScheduledTask)
}
```

Feature-owned registrations are released in last-in-first-out order. Jobs launched in `coroutineScope` are owned automatically; platform scheduler tasks must be registered explicitly. The resource registry must cover:

- Paper and Velocity listeners.
- Cloud root commands.
- CharonFlow subscriptions.
- Service hooks.
- Menu buttons and pages.
- Bukkit recipes.
- GUI scopes.
- Scheduler tasks.

Registration-oriented APIs return a closeable handle:

```kotlin
interface Registration : AutoCloseable
```

Feature code registers those handles with its context:

```kotlin
context.own(menu.registerButton(...))
context.own(messaging.subscribe(...))
context.own(service.registerHook(...))
```

The coroutine scope and feature-owned Koin modules are Kernel-owned lifecycle resources rather than generic LIFO registrations. Calling the internal managed context's `close()` prevents further registration and drains all feature-owned registration handles exactly once.

The disable cleanup sequence is:

1. Invoke feature `onDisable`.
2. Close the internal managed context even if the hook failed, draining tracked registrations in reverse order.
3. Cancel the feature coroutine scope.
4. Unload feature-owned dependency injection definitions.
5. Remove the feature instance.
6. Store and report the complete operation result.

## Cloud Command Ownership

Cloud supports deleting root commands through `deleteRootCommand`. Dynamic command ownership follows these rules:

- Every feature declares the root commands it owns.
- Two dynamic features must not own the same root command.
- The fixed `/plutoproject` command belongs to the Kernel.
- Feature disable removes every root owned by that feature.
- Feature-specific parsers and suggestions should be attached directly to feature commands where possible.
- Global named parsers must not retain references to a feature instance that can be disabled.

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

Loads common and current-platform manifests, preserves descriptor source information, and reports malformed resources or unsupported schemas.

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

Creates immutable plans before lifecycle execution:

```kotlin
data class ActivationPlan(
    val capabilities: List<ModuleDescriptor>,
    val features: List<ModuleDescriptor>,
)
```

### RuntimeModuleManager

Serializes lifecycle operations, executes plans, owns states and contexts, and applies failure propagation.

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
    suspend fun load(id: String): ModuleOperationResult
    suspend fun disable(id: String): ModuleOperationResult
    suspend fun reload(id: String): ModuleOperationResult
}
```

```kotlin
interface CapabilityController {
    suspend fun retry(id: String): ModuleOperationResult
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

Cross-feature integrations depend on the provider's API project and obtain services through normal dependency injection.

Migrated runtime modules must not call raw platform listener, command, scheduler, recipe, global Koin, or subscription registration APIs. Architecture checks and migration reviews enforce use of approved Kernel ownership wrappers; the context can only clean resources registered through those wrappers.

## Administrative Commands

```text
/plutoproject feature list
/plutoproject feature info <id>
/plutoproject feature load <id>
/plutoproject feature disable <id>
/plutoproject feature reload <id>

/plutoproject capability list
/plutoproject capability info <id>
/plutoproject capability retry <id>

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
plutoproject.command.feature.load
plutoproject.command.feature.disable
plutoproject.command.feature.reload
plutoproject.command.capability.list
plutoproject.command.capability.info
plutoproject.command.capability.retry
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
- Generate common, Paper, and Velocity manifests.
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
- Implement explicit capability retry.
- Implement fresh feature instances after disable or failed load.
- Implement the standalone reload hook.
- Serialize lifecycle operations.
- Validate duplicate IDs within each platform discovery set, missing dependencies, platform compatibility, illegal dependency types, and required-edge cycles.

Required tests:

- A -> B -> C runs `C.load, B.load, A.load, C.enable, B.enable, A.enable`.
- A preloaded before a required dependency enable failure is cleaned and returned to `DISCOVERED` with a blocked result.
- A, B, C can only be disabled in A, B, C order.
- Disabling B while A is enabled returns the A -> B blocker path.
- An optional B that is already enabled creates an active edge.
- An optional B that is disabled is not loaded automatically.
- If A fails after starting B, B remains enabled.
- Capability disable is unavailable during runtime.
- A capability remains enabled after every consumer is disabled.
- A failed capability retries only through explicit retry.
- Reload invokes no other lifecycle hooks.
- Reload failure leaves the feature enabled.
- Loading a disabled feature creates a new instance.
- The same ID in separate PAPER and VELOCITY descriptors is accepted, while a duplicate within COMMON plus the current platform is rejected.

Acceptance criteria:

- The graph and manager are platform-independent and unit tested.
- No manager operation recursively calls another public lifecycle operation.
- Operation results retain phase, cause, and dependency path details.

Suggested commit:

```text
feat(kernel): 实现运行时模块管理器
```

### Phase 4: Implement Platform Resource Contexts

Tasks:

- Create `kernel/paper` and `kernel/velocity`.
- Wire the new Kernel into Paper and Velocity platform lifecycle entrypoints.
- Keep the old FeatureManager temporarily for unmigrated legacy features, but route newly migrated runtime modules only through the new Kernel.
- Keep `/plutoproject` management operations hidden until their target modules satisfy resource ownership requirements.
- Implement platform-specific `ModuleContext` types.
- Track and unregister Paper and Velocity listeners.
- Wrap Cloud command registration and root command deletion.
- Track Koin module installation and unloading.
- Automatically own jobs launched in the module coroutine scope.
- Add ownership wrappers for Paper and Velocity scheduler tasks.
- Add ownership handles for GUI scopes, menu registrations, recipes, subscriptions, and service hooks.
- Generalize registration handles for subscriptions and hooks.
- Add ownership support to menu registration APIs.
- Track and remove Bukkit recipes.
- Guarantee Kernel cleanup after `onDisable` failure.

Acceptance criteria:

- Disabled features no longer receive events.
- Disabled feature command roots are removed.
- Loading a feature again does not duplicate commands or Koin definitions.
- Coroutine jobs and subscriptions are cancelled.
- One cleanup failure does not prevent remaining cleanup actions.
- A test runtime module can be discovered and managed through the real Paper and Velocity Kernel bootstrap.

Suggested commit:

```text
feat(kernel): 实现模块资源生命周期管理
```

### Phase 5: Add Administrative Commands

Tasks:

- Register the fixed `/plutoproject` command in each platform Kernel.
- Implement feature list, info, load, disable, and reload.
- Implement capability list, info, and retry.
- Implement module graph inspection.
- Add command permissions.
- Add command messages to platform `Message.kt` files.
- Display dependency blockers and the latest operation error.

Acceptance criteria:

- Commands do not modify configuration files.
- Concurrent lifecycle commands are serialized.
- Disable rejection reports accurate dependency paths.
- Capability retry reports each dependency retry result.
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
- Declare transitive capability dependencies.
- Replace global companion service lookup with dependency injection.
- Register owned resources with `ModuleContext`.
- Add new-structure unit tests where applicable.
- Remove the corresponding unconditional legacy initialization path as soon as the replacement capability is wired.

Acceptance criteria:

- `enable-features = []` does not read external connection configuration.
- MongoDB is not initialized without a consumer.
- CharonFlow is not initialized without a consumer.
- Interactive listeners are not registered without a consumer.
- Server statistics is not initialized without a consumer.
- World alias configuration is not read without a consumer.
- Successfully enabled capabilities close only during server shutdown; failed partial instances are cleaned immediately.
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
- Replace global MongoConnection and global Koin access.
- Move registrations into `ModuleContext`.
- Record the existing core, API, and gameplay behavior that must be preserved or restored by final acceptance.

Migration checkpoint:

- Existing unit tests remain available as final-regression coverage; run relevant subsets during migration when useful.
- Paper and Velocity descriptors are generated correctly.
- Track any remaining resource-ownership work required before the features can be disabled and loaded again without duplicates.
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
- A feature is not considered migrated until all listeners, commands, Koin definitions, menu registrations, recipes, subscriptions, and jobs can be removed on disable.

Migration tracking for every wave:

- Record runtime ID and dependency changes so final compatibility can be verified.
- Record any temporarily broken cross-system integration or gameplay behavior that must be restored before final acceptance.
- Record remaining unowned runtime resources that must be moved behind `ModuleContext` before final acceptance.

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
- Add a source audit check that rejects direct listener, command, scheduler, recipe, global Koin, and subscription registration calls from migrated runtime modules outside approved Kernel wrapper packages.
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

1. Start with `enable-features = []` and no database configuration.
2. Load a feature without database requirements and verify MongoDB and Redis remain inactive.
3. Load a MongoDB-dependent feature and verify Mongo starts on demand.
4. Make Mongo initialization fail and verify only dependent features are blocked.
5. Fix Mongo and recover it through `/plutoproject capability retry mongo`.
6. Load A where A -> B -> C and verify C, B, A activation order.
7. Attempt to disable B while A is enabled and verify the A -> B blocker path.
8. Disable A, B, and C in that order.
9. Verify capabilities remain enabled after every consumer is disabled.
10. Reload a feature and verify only its reload hook executes.
11. Disable a feature and verify its commands, listeners, menus, recipes, subscriptions, and jobs disappear.
12. Load the feature again and verify a fresh instance starts without duplicate registrations.
13. Shut down the server and verify features close before capabilities.

## Completion Criteria

The refactor is complete when:

- The fixed Kernel is the only unconditional runtime layer.
- Feature and capability lifecycle is managed by one runtime manager.
- Capabilities activate from static requirements and remain active until shutdown.
- Runtime feature management follows the dependency and reload semantics in this specification.
- Every dynamic feature resource has explicit ownership and cleanup.
- All legacy flat projects and old conventions have been removed.
- Existing DDD features use the simplified project names.
- One unified distribution JAR contains the Paper and Velocity platforms, dependency manifests, and runtime descriptors.
- The complete test and Shadow JAR builds pass.
