# Runtime Module System Refactor

## Status

- Status: Phase 6 capability extraction and Phase 7 module isolation completed
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

kernel/
  module-processor/

catalog/
```

Only projects that contain real code are created. Ancillary build or publication projects such as `catalog`, and feature-owned projects such as `frontend`, are outside the runtime module hierarchy but remain part of the build.

The root build project is the single distribution project. It aggregates `platform/paper` and `platform/velocity`, and produces one JAR containing both platform entrypoints, embedded runtime dependencies, and runtime module descriptors. The two platform projects are composition roots inside that unified artifact; they are not separate published distributions.

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
| Kernel API | Foundation, Koin core, and platform-independent coroutine, logging, DI, and service contracts exposed by `ModuleContext` |
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
    id = "database_persist",
    platform = Platform.PAPER,
    requiredCapabilities = ["mongo"],
)
```

Both annotations generate a unified descriptor:

```kotlin
@Serializable
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

`ModuleDescriptor`, `ModuleType`, and `Platform` are the single serialization
schema for runtime descriptor resources. The processor encodes descriptors with
Kotlinx Serialization and `encodeDefaults = true`, so all dependency arrays are
written even when empty. Discovery decodes the same type with
`ignoreUnknownKeys = true`, then validates `schemaVersion` explicitly. No
processor-local DTO or hand-written JSON encoder/decoder duplicates the schema.

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

The descriptor's `type` field distinguishes features from capabilities; it is not part of the resource filename because feature and capability IDs share one namespace within a platform. `ModuleDiscovery` reports unknown schema versions and malformed files while retaining source paths. `ModuleDescriptorValidator` rejects duplicate IDs within the current platform discovery set. The same ID may have separate PAPER and VELOCITY descriptors because each Kernel sees only its own platform set. The distribution build rejects duplicate descriptor resource paths before packaging. Any descriptor or static graph validation error aborts platform plugin startup before lifecycle execution and is reported through logs. The KSP processor is moved out of the framework implementation and into `kernel/module-processor`.

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
- Module-local dependency injection container creation and closure.
- Cross-module service registration ownership and revocation.
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

### Lifecycle Cancellation

`CancellationException` is control flow, not a module failure. Lifecycle code
never converts it to `ModuleOperationResult.Failed`, including when a module
hook throws it directly while the caller's job is still active. The manager
finishes the required cleanup in a `NonCancellable` context and then rethrows
the original cancellation.

Cancellation of `loadStartup` or `enableStartup` is terminal for that manager
instance. The manager rejects further startup or runtime operations and cleans
up every module instance created by the interrupted startup in reverse
activation order. Modules whose `onLoad` completed receive `onDisable`; the
module whose `onLoad` was interrupted does not receive `onDisable`, matching
ordinary load-failure semantics, but its Kernel-owned scope is still cancelled
and joined. Created modules end in `DISABLED`, modules that were never created
remain `DISCOVERED`, and every `runningOperation` is cleared. Startup is not
retryable after cancellation.

Cancellation of runtime `disable` does not interrupt resource release. The
manager completes scope cleanup, removes active optional edges, records the
module as `DISABLED`, and then rethrows the cancellation. Once `shutdown` has
started, caller cancellation likewise cannot stop the reverse shutdown pass;
all remaining modules are attempted before cancellation is rethrown.

If cleanup produces ordinary failures while cancellation is already in
progress, the original `CancellationException` remains the primary exception.
Cleanup failures are reported through the registry and operation reporter,
attached to the cancellation as suppressed exceptions, and do not prevent
cleanup of later modules.

Every `ModuleContext.coroutineScope` is owned by that module's lifecycle. Its
job is distinct from the job invoking manager lifecycle functions, is created
with the module, and is cancelled and joined when the module is cleaned up.

The default scope carries the module context across coroutine suspension and
thread switches. A separately created scope, `CompletableFuture`, scheduler,
thread-pool callback, listener, or command does not inherit that identity and
must retain an explicit `ModuleContext`.

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
| Dependency enable failure after dependent load | Revoke exports, invoke dependent `onDisable` best-effort, close its scope and Koin container, discard the instance, and set `BLOCKED` |
| `onLoad` | Do not invoke `onDisable`; revoke exports, close the Kernel-owned scope and Koin container, discard the instance, and set `FAILED` |
| `onEnable` | Revoke exports, invoke best-effort `onDisable`, close the scope and Koin container, discard the instance, and set `FAILED` |
| `onDisable` | Revoke exports, close the scope and Koin container, discard the instance, end in `DISABLED`, and return a failed operation result |

Successfully activated dependencies remain enabled when a dependent feature fails.

## Server Shutdown

Server shutdown does not use the interactive blocker policy.

Shutdown order:

1. Disable active features in reverse active-graph order.
2. Revoke feature services, cancel feature scopes, close feature Koin containers, and destroy feature contexts and instances.
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
    val koin: Koin
    val services: ModuleServices

    fun saveResource(
        path: String,
        output: Path = Path.of(path),
        resourcePrefix: String? = null,
        replace: Boolean = false,
    ): Path
}
```

`saveResource` resolves resources from `module/<module-id>/<path>` by default.
A non-null `resourcePrefix` replaces `module/<module-id>`. Resource paths and
prefixes are normalized relative paths and reject empty values and `..`
segments. Relative output paths resolve under `dataFolder`; absolute output
paths are used unchanged. The Kernel creates output parent directories and
uses the same class loader as module discovery and runtime construction.

Existing output files are returned unchanged unless `replace` is true. Writes
use `Files.copy` directly; concurrent creation conflicts propagate to the
caller. Missing packaged resources fail immediately with an exception that
identifies the module and complete resource path. This helper does not transfer
ownership of the resulting file to the Kernel: modules remain responsible for
any cleanup their behavior requires.

The Kernel creates the module coroutine scope and module-local Koin application,
and therefore always cancels and joins the scope and closes the Koin application
when the module ends. The Kernel also revokes services exported through
`ModuleServices` and clears its own state and instance references. Listeners,
commands, scheduler tasks, recipes, GUI state, subscriptions, platform service
hooks, database clients not owned by Koin definitions, and all other side
effects created directly by module code remain the module's responsibility.

Feature and capability implementations should clean their side effects in `onDisable`, but the Kernel does not enforce or verify complete cleanup. Existing features may be structurally migrated without adding missing cleanup behavior. A module may therefore leave registrations or other effects behind after reaching `DISABLED`; conflicts and residue are defects or limitations of that module rather than Kernel state inconsistencies.

The disable cleanup sequence is:

1. Mark the owner-bound service view as closing and atomically revoke every service exported by the module.
2. Invoke module `onDisable` best-effort while service queries and the module Koin container remain available.
3. Cancel and join the Kernel-created module coroutine scope even if the hook failed.
4. Close the module-local Koin application.
5. Close the service view and unregister current-context and entrypoint mappings.
6. Remove the context and instance, then store and report the complete operation result.

Every stage is attempted. The first ordinary cleanup failure remains primary
and later failures are suppressed. When cancellation is in progress, the
original `CancellationException` remains primary and cleanup failures are
suppressed. Shutdown continues with other modules. Scope shutdown has no
timeout; a module that remains forever in `NonCancellable` can block cleanup and
is considered defective module behavior.

## Module-Local Dependency Injection

Koin is part of the Runtime Module public API. `kernel/api` exposes Koin types
and has an API dependency on Koin core. Each live runtime module receives one
isolated `KoinApplication`; the Kernel owns that application and modules receive
only its `Koin` through `ModuleContext.koin`.

The container is created before the runtime entrypoint is constructed, starts
empty, and remains the same through construction, `onLoad`, `onEnable`, and
`onDisable`. The Kernel does not prebind `ModuleContext`, logger, path, scope,
clock, or platform context. A module may bind captured values itself. Calling
`injectModule<ModuleContext>()` therefore fails unless the module defines that
binding. Koin logging is bridged to the module logger.

The public convenience API is in `plutoproject.kernel.api`:

```kotlin
context.injectModule<T>(qualifier, mode, parameters)
injectModule<T>(qualifier, mode, parameters)
context.getModule<T>(qualifier, parameters)
getModule<T>(qualifier, parameters)

context.loadModuleDefinitions(modules, allowOverride, createEagerInstances)
loadModuleDefinitions(modules, allowOverride, createEagerInstances)
context.unloadModuleDefinitions(modules)
unloadModuleDefinitions(modules)
```

The injection helpers forward Koin qualifiers and parameters.
`injectModule` also exposes `LazyThreadSafetyMode`, defaulting to
`SYNCHRONIZED`. Definition loading does not perform additional conflict checks:
`allowOverride` defaults to true. The helper creates `createdAtStart` instances
immediately by default, while direct `context.koin` calls retain native Koin
semantics. Definition loading is allowed during entrypoint construction and all
lifecycle phases while the container is open.

The Kernel owns container closure. Modules must not close the Koin instance,
take over its lifecycle, or unload definitions owned by the Kernel. This is an
API contract rather than a restricted proxy. Koin-owned resources should use
definition close callbacks so container closure releases them.

### Current Module Context

The same root API package provides strict and nullable context lookup:

```kotlin
currentModuleContext()
currentModuleContextOrNull()
```

Explicit `ModuleContext` usage is preferred. No-receiver helpers are a
convenience for entrypoint construction and lifecycle code. Resolution order is:

1. The dynamically bound current module context.
2. A `StackWalker` match for the nearest registered `RuntimeModule` entrypoint.
3. A descriptive failure, or null for `currentModuleContextOrNull`.

Resolution never infers ownership from package names, the shared class loader,
or the most recently active module. If the nearest entrypoint belongs to a
closed module, resolution stops there instead of selecting an outer active
module. Runtime descriptors are internal and are expected to use distinct
entrypoint classes; the Kernel adds no duplicate-entrypoint validation.

Dynamic binding is nested, exception-safe, and restores the previous context in
LIFO order. The Kernel binds it around entrypoint class initialization,
construction, and every lifecycle callback. A `ModuleContextElement` propagates
the binding through Kernel lifecycle calls, the default module scope, and
inherited coroutine contexts such as `withContext(Dispatchers.IO)`.

An `@InternalKernelApi` opt-in SPI, with `RequiresOptIn` error level, allows
`kernel/common` to register and bind contexts for helpers implemented in
`kernel/api`. It stores only active context and entrypoint mappings, never
business services.

## Cross-Module Service Registry

Each `RuntimeKernel` owns one thread-safe Service Registry. It is not a JVM
global and is not Koin `GlobalContext`. `ModuleContext.services` is an
owner-bound `ModuleServices` view; modules cannot obtain the raw Registry or
forge an owner ID.

The Registry is intentionally for singleton, non-generic service interfaces.
Its only key is `KClass<T>`, with exactly one active provider per type. It has no
qualifiers, generic type tokens, or multi-provider support. Generic service
interfaces must instead be expressed as distinct contracts such as
`UserRepository` and `OrderRepository`; this is a documented authoring rule,
not a runtime reflection check.

The core API accepts `KClass<T>` and provides reified Kotlin extensions:

```kotlin
services.exportService(type, instance)
services.exportService(type, qualifier, parameters)
services.getService(type)
services.getServiceOrNull(type)
```

The Koin-backed export resolves the local definition exactly once, even for a
factory, and registers that concrete instance. Both export forms return a
minimal `ServiceRegistration` exposing `serviceType`, `isRegistered`, and an
idempotent `unregister()`. A duplicate active type always fails, including a
duplicate from the same owner. After unregistering, an active owner may export
the type again; an old registration cannot unregister its replacement.

The Registry never closes exported instances, including `AutoCloseable`
objects. Resource ownership remains with the provider module. It automatically
revokes all registrations when the provider begins cleanup. A retained service
reference after removal remains the consumer's responsibility.

Any active module may discover any currently registered service. Registry use
does not create dependency edges, change blocker paths, or replace descriptor
dependencies. Service publication is dynamic and immediately visible whenever
the owner is active. The Kernel does not require providers to publish in a
specific phase; consumers must handle a service that has not yet been
published.

Owner-bound service states are:

- `INITIALIZING`: queries are allowed during entrypoint construction, but exports are forbidden.
- `ACTIVE`: queries, exports, unregister, and re-export are allowed.
- `CLOSING`: new exports are forbidden; queries remain available during `onDisable` and scope cleanup.
- `CLOSED`: every operation fails, including nullable queries.

Missing `getService` calls and invalid owner states use descriptive standard
exceptions rather than public custom exception types. Modules cannot enumerate
the Registry. Kernel internals retain read-only snapshots for tests and future
management diagnostics.

An optional `importService<T>(localQualifier)` helper creates and loads a local
Koin factory whose resolution queries the owner-bound service view. It returns
the generated Koin `Module` for symmetric unloading. The local qualifier only
identifies the consumer's Koin definition; the Registry remains keyed by service
type. Import conflicts follow definition loading's default override behavior.
Direct Registry queries never add definitions to Koin.

## Runtime Kernel Process Ownership

At most one `RuntimeKernel` may be active in a JVM. Construction does not claim
the process slot; the first `load()` claims it atomically. A second active
Kernel fails immediately. Result-valued module failures leave the Kernel active
for management and shutdown. Normal shutdown releases the slot after all live
contexts are gone. An exception or cancellation escaping startup first performs
complete reverse cleanup and releases the slot only after no live context
remains.

## Cloud Commands

The fixed `/plutoproject` command belongs to the Kernel and remains available independently of feature state. New feature commands use each platform's native Brigadier API and do not depend on Cloud.

Migrated runtime modules that still contain legacy Cloud annotation commands may temporarily require the `legacy_cloud_commands` capability. This capability is platform-specific and has separate Paper and Velocity implementations under one process-local runtime ID. Its thin platform API exposes an `AnnotationParser` through a non-generic service contract; it does not belong in Kernel or Foundation, and legacy modules that have not entered the runtime module system continue using the legacy Framework path.

The consuming feature owns every command root that it registers. It records the roots returned by `AnnotationParser.parse`, and its `onDisable` deletes each root through `parser.manager().deleteRootCommand`. Registration must compare the command manager's roots before and after parsing; if parsing fails, the feature deletes every root introduced by that attempt before rethrowing. The capability does not track or unregister consumer commands during its own shutdown.

A Cloud root and all of its aliases may have only one feature owner. Multiple command containers in the same feature may intentionally contribute to the same root and are removed together. Different features must not contribute subcommands to one root because Cloud can only delete the complete root tree. Such commands must move to one owner or be migrated to native Brigadier.

This capability is transitional. It accepts no new commands, and it will be removed after all Cloud command consumers have migrated to native Brigadier.

The migrated whitelist Paper module temporarily falls back to the current world's native spawn when a visitor disconnects. Its previous default-warp lookup remains disabled until the legacy warp feature has a new runtime API; whitelist must not depend on the flat legacy feature API in the interim.

The migrated gallery Paper module temporarily omits its legacy menu button registration. The original integration remains commented in `GalleryFeature.kt`, and the button implementation remains commented in `ImageListMenuButton.kt`, until the legacy menu feature exposes a runtime-module API. Gallery must not depend on the flat legacy menu API in the interim. Its standalone commands, upload server, display runtime, recipes, and listeners remain active.

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

Cross-module integrations depend on the provider's API project and obtain
singleton contracts through `ModuleServices`. A consumer may optionally import
such a service into its isolated module-local Koin container. Neither mechanism
creates runtime dependency edges; descriptors remain the source of dependency
ordering and disable blockers.

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
| `distribution` | Unified Paper and Velocity Shadow JAR and build manifest |

Conventions configure build behavior. They do not add MongoDB, Koin, Ktor, CharonFlow, LuckPerms, or other business dependencies implicitly.

Additional rules:

- Repositories are centralized in `settings.gradle.kts`.
- KSP, Kapt, Compose, and serialization are applied only where used.
- Every project declares its direct dependencies explicitly.
- Gradle coordinates are derived deterministically from the complete project path.
- `:feature:home:paper` uses a coordinate equivalent to `club.plutoproject.feature.home:paper`.
- Runtime dependencies are declared with `implementation` and embedded in the unified Shadow JAR. The build does not generate dependency lists, download libraries at runtime, or add downloaded paths to a platform classloader.
- Embedded third-party dependencies are currently not relocated. Paper plugin classloader isolation prevents these classes from entering unrelated Paper plugins unless they explicitly depend on PlutoProject.
- The root distribution project packages both platform composition roots and their runtime dependencies into one JAR.
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

Status: Complete (2026-07-11). The build results, packaged legacy manifests,
and dependency graph are recorded in
[`runtime-module-system-baseline.md`](runtime-module-system-baseline.md).

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

Status: Complete (2026-07-11). The orthogonal Kotlin, test, Paper, Velocity,
and distribution conventions are available. Gallery and whitelist-v2 core/API
projects are the first migrated projects; legacy conventions remain only for
projects scheduled for later migration phases. Repository declarations are
centralized in `settings.gradle.kts`, and `verifyArchitecture` establishes the
initial project path, coordinate, plugin isolation, and dependency direction
checks. Runtime-module processor wiring remains deferred to Phase 2.

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

Status: Complete (2026-07-11). `kernel/api`, its Paper and Velocity extensions,
and `kernel/module-processor` now define and generate the unified runtime
module protocol. The `plutoproject.runtime-module` convention wires KSP with
project identity. Processor unit and Gradle TestKit functional tests cover valid
descriptor generation and invalid metadata/entrypoint shapes, including object
declarations. Legacy feature descriptors remain active until the migration and
platform cutover phases.

Tasks:

- Create `kernel/api`.
- Create `kernel/api/paper` and `kernel/api/velocity`.
- Define `RuntimeModule`, module contexts, descriptors, states, and operation results.
- Define `@Feature` and `@Capability`.
- Create `kernel/module-processor`.
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

Status: Complete (2026-07-11). `kernel/common` now provides descriptor
discovery and validation, immutable activation planning, required and active
optional dependency graphs, reflective module creation, registry/reporting,
and a serialized runtime manager. Seventeen focused tests cover lifecycle
ordering, failure propagation and cleanup, blocker paths, optional-plan cycles,
sticky capabilities, permanent feature disable, discovery errors, platform ID
isolation, and static validation before instance creation.

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

Status: Complete (2026-07-12). `kernel/paper` and `kernel/velocity` now discover
and manage runtime descriptors from the real platform lifecycle. Paper runs the
load stage from plugin `onLoad`; Velocity runs it during plugin construction;
both run enable and shutdown from their corresponding platform events. The
legacy manager remains active for legacy manifests, while platform bootstrap
tests verify descriptor discovery, lifecycle execution, and cancellation of
Kernel-owned module jobs on both platforms.

Tasks:

- Create `kernel/paper` and `kernel/velocity`.
- Wire the new Kernel into Paper and Velocity platform lifecycle entrypoints.
- Run module `onLoad` from Paper plugin `onLoad` and Velocity plugin construction.
- Run module `onEnable` from Paper plugin `onEnable` and the Velocity proxy initialization event.
- Keep the old FeatureManager temporarily for unmigrated legacy features, but route newly migrated runtime modules only through the new Kernel.
- Create the minimal platform module contexts required to expose platform APIs without resource ownership wrappers.
- Create and cancel one Kernel-owned coroutine scope per module.
- Keep module-created listeners, commands, tasks, recipes, subscriptions, GUI state, hooks, and direct resources outside Kernel ownership; Kernel owns only the module Koin application and definitions through container closure.
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

Status: Complete (2026-07-12). Paper registers a native Brigadier command tree
through `LifecycleEvents.COMMANDS`, and Velocity registers the equivalent tree
through its native `BrigadierCommand` API. The commands expose feature and
capability listing and inspection, permanent feature disable, dependency graph
paths, per-command permissions, dynamic module ID suggestions, blocker paths,
and latest lifecycle failure summaries without modifying configuration.

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

Status: Completed (2026-07-13). All eight Phase 6 capabilities have been extracted
with lifecycle-owned implementations and runtime descriptors. Paper-only
capabilities now include `interactive`, `server_statistics`, and `world_alias`;
the latter two export their explicit API services through ModuleServices. Legacy
consumers and their old framework paths remain during the transition until new
runtime feature descriptors declare the required capabilities. This preserves
legacy compatibility without activating a capability when the Kernel has no
runtime consumer.

Extract capabilities in dependency order:

| Order | Capability | Current source |
| --- | --- | --- |
| 1 | `mongo` | `framework-common/connection` |
| 2 | `charonflow` | `framework-common/connection` |
| 3 | `geoip` | `framework-common/connection` |
| 4 | `database_persist` | `framework-common/databasepersist` |
| 5 | `profile` | `framework-common/profile` and the Velocity profile listener |
| 6 | `interactive` | `framework-paper/interactive` |
| 7 | `server_statistics` | `framework-paper/statistic` and its API contracts |
| 8 | `world_alias` | `framework-paper/worldalias`, its API contracts, and configuration |

Classification decisions:

- The platform command manager belongs to the Kernel because `/plutoproject` always requires it.
- Server statistics is the Paper `server_statistics` capability. Its consumers are `status`, `dynamic-scheduler`, and `overload-warning`.
- WorldAlias is the Paper `world_alias` capability. Its consumers are `teleport`, `home`, and `warp`.
- Toast and stateless Paper DSL code move to `foundation/paper`.
- BuildInfo belongs to the Kernel.
- Stateless configuration loading code moves to `foundation/common`.

Database-persist migration decisions:

- `database_persist` requires both `mongo` and `server_identifier`
  capabilities (`server_identifier` is the runtime ID for the server-identity
  capability).
- The server identifier is required to be non-null. The capability must fail to
  load rather than silently change cross-server change filtering when no
  identifier is configured.
- Long-lived jobs must use the owning `ModuleContext.coroutineScope`. The
  capability must not use the legacy global `PluginScope`, create a new
  top-level `SupervisorJob`, or depend on the legacy virtual-thread dispatcher.
- Database-persist implementations receive their scope, logger, Mongo service,
  server identifier, and platform conditions through explicit construction or
  module-local Koin bindings. The Kernel does not pre-bind these values.
- Database-persist must close its change stream, subscriptions, containers, and
  child jobs during capability shutdown before Kernel scope cleanup.
- `BsonSerializer` and `UuidAsBsonBinarySerializer` are shared by multiple
  Mongo-backed modules and move to `foundation/common`.
- BSON dot-path helpers remain private to database_persist until another module
  demonstrates a concrete need for them. Concurrent collection wrappers and
  trivial time conversion helpers are inlined instead of becoming Foundation
  APIs. Resource-owning dispatchers, global loggers, `globalKoin`, and server
  identity configuration do not belong in Foundation.
- The legacy global `DatabasePersist` API and implementation remain in place
  during this migration. New consumers use the capability service export and
  declare their capability dependency; the old path is removed only after
  consumers have been cut over. A player/data owner must not be served by both
  implementations during the transition.

Tasks for each capability:

- Create only the required API, common, Paper, and Velocity projects.
- Move contracts out of the old framework API.
- Move implementations and configuration into the capability.
- Add `@Capability` to the runtime entrypoint.
- Add thin Paper and Velocity runtime entry projects when a capability uses the same common implementation on both platforms.
- Declare transitive capability dependencies.
- Prepare explicit capability API contracts; module-local DI and service export cutover are tracked in Phase 7.
- Keep capability-created resources under the capability's own lifecycle responsibility.
- Add new-structure unit tests where applicable.
- Remove the corresponding unconditional legacy initialization path as soon as the replacement capability is wired; retain legacy compatibility paths until their consumers are cut over.

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

Capabilities may be committed separately to keep reviews and regressions focused; related final migrations may also be grouped when their verification is shared.

### Phase 7: Isolate Module DI and Add the Service Registry

Status: Completed (2026-07-13). Module-local Koin, current-context propagation,
the Service Registry, cleanup, and process-ownership contracts are implemented;
Mongo exports its connection through the module service registry.

Kernel API tasks:

- Make Koin core an API dependency of `kernel/api` and expose `koin` and owner-bound `services` from `ModuleContext`.
- Add explicit-context and no-receiver `injectModule`, `getModule`, `loadModuleDefinitions`, and `unloadModuleDefinitions` helpers.
- Add `currentModuleContext`, `currentModuleContextOrNull`, and coroutine context propagation.
- Add the error-level `@InternalKernelApi` SPI used by Kernel common to register and bind active contexts.
- Add `ModuleServices`, `ServiceRegistration`, KClass-based operations, and reified Kotlin conveniences in the root API package.
- Add optional `importService` support without coupling the Registry key space to Koin qualifiers.

Kernel implementation tasks:

- Create one empty isolated `KoinApplication` per live module before entrypoint construction and bridge its logging to the module logger.
- Keep the same application available through construction and every lifecycle hook, then close it on every cleanup path.
- Implement nested dynamic context binding, nearest-entrypoint StackWalker fallback, and `ModuleContextElement` propagation.
- Create one thread-safe Service Registry per RuntimeKernel and expose only owner-bound views to modules.
- Enforce `INITIALIZING`, `ACTIVE`, `CLOSING`, and `CLOSED` service-view states.
- Make export, duplicate detection, unregister, owner close, and unregister/re-export races atomic.
- Revoke owner exports before `onDisable`, keep queries and Koin available through scope cleanup, then close Koin and unregister context mappings.
- Preserve cancellation as the primary exception and aggregate later cleanup failures as suppressed while continuing shutdown.
- Fix context-created-before-live-instance failure paths so cleanup failures never replace the original cancellation.
- Enforce one active RuntimeKernel per JVM from first `load()` until complete shutdown or failed-startup cleanup.

Mongo migration tasks:

- Remove `GlobalContext` loading and unloading from `MongoCapability`.
- Define `MongoConnection` in the module-local Koin container with an `onClose` callback that closes the connection.
- Export the resolved singleton through `ModuleServices` during capability load.
- Remove the capability's connection field and manual disable cleanup.

Required tests:

- Module Koin containers are isolated, start empty, forward qualifiers and parameters, apply definition override/eager defaults, and always close.
- Dynamic current-context binding nests and restores correctly across success, failure, cancellation, suspension, and dispatcher switches.
- StackWalker resolves only the nearest registered entrypoint and never skips a closed entrypoint.
- Independent callbacks and scopes require an explicit context, and closed modules are no longer discoverable.
- A second RuntimeKernel cannot load while another holds the process slot; sequential Kernels work after complete shutdown.
- Service export, query, nullable query, duplicate rejection, unregister, re-export, and registration identity behave deterministically.
- Constructor-time queries work while exports fail; closing permits queries but forbids exports; closed views reject every operation.
- Concurrent export-versus-close and duplicate-export races cannot leak registrations.
- Lifecycle failures and cancellation revoke exports, stop the scope, close Koin, unregister context identity, and retain correct primary/suppressed failures.
- `importService` resolves the current Registry service and can be unloaded symmetrically.
- Mongo exports one `MongoConnection`, does not touch GlobalContext, and closes it through Koin application shutdown.

Acceptance criteria:

- No Runtime Module adds definitions to Koin GlobalContext.
- Every live module has exactly one Kernel-owned Koin application and owner-bound service view.
- Cross-module services are explicit singleton exports rather than access to another module's Koin container.
- Registry use does not mutate descriptor dependency edges or runtime disable blockers.
- Module cleanup leaves no active service registrations, Koin application, default-scope job, or current-context mapping.
- All failure and cancellation paths attempt every Kernel-owned cleanup stage.

Commit boundaries:

```text
feat(kernel): 添加模块隔离依赖注入与服务注册表
refactor(mongo): 使用模块服务注册表导出连接
```

### Phase 8: Rename Existing DDD Feature Projects

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
- Replace global MongoConnection access with the `mongo` capability service export and module-local dependency injection.
- Record the existing core, API, and gameplay behavior that must be preserved or restored by final acceptance.

Migration checkpoint:

- Existing unit tests remain available as final-regression coverage; run relevant subsets during migration when useful.
- Paper and Velocity descriptors are generated correctly.
- Track any temporary Gallery frontend assembly breakage that must be restored before final acceptance.

Suggested commit:

```text
refactor(feature): 简化新版功能模块命名
```

### Phase 9: Migrate Legacy Features

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

### Phase 10: Cut Over Platform Bootstrap

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
- The unified distribution contains both platform entrypoints, embedded runtime dependencies, and descriptors for both platforms.

Suggested commit:

```text
refactor: 切换到统一运行时模块系统
```

### Phase 11: Enforce Architecture and Update Documentation

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
- One unified distribution JAR contains the Paper and Velocity platforms, embedded runtime dependencies, and runtime descriptors.
- The complete test and Shadow JAR builds pass.
