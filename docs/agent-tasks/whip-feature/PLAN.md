# Implementation Plan: Whip Feature

## References
- Specification: `SPEC.md`

## Progress
- [x] Slice 1 — Players and administrators can obtain and upgrade valid whips
- [x] Slice 2 — Holding use deploys a terrain-aware physical whip
- [x] Slice 3 — Swept whip motion damages and knocks back multiple targets with audio feedback

## Current codebase state
- `settings.gradle.kts` registers runtime Features through `includeFeature`; no `whip` Gradle project exists yet.
- Paper-only feature projects apply `plutoproject.paper` (or the dev-bundle variant when NMS is necessary) and `plutoproject.runtime-module`.
- Runtime Modules register listeners, recipes, and commands during lifecycle callbacks and explicitly unregister them on disable.
- The project targets Paper `26.2`, whose API exposes item data components, the trident item-use animation, Display entities, collision shapes, and standard attributed damage entry points.
- Administrative commands use `PaperLegacyCloudCommands` and `CloudCommandRegistration`; adding the command makes `legacy_cloud_commands` a required Capability.
- Existing custom items use namespaced persistent data and Paper data components. Gallery crafting listeners demonstrate custom ingredient validation for players and automatic crafters.
- `foundation:paper` already provides `Entity.coroutineDispatcher`, backed by `EntityDispatcher`/`EntityScheduler` on Folia and the server event loop on Paper, plus chunk and global counterparts.
- Folia's default region sections are far larger than the default 10-block whip, but distance and view range are not ownership guarantees. The core loop must use the same entity-dispatcher and ownership-check path on every runtime; runtime-specific checks are reserved for integration points with genuinely different API behavior.
- The repository has no resource-pack or custom-model infrastructure.
- No automated server harness exists for observing client poses, Display interpolation, or live rope behavior. Validation therefore uses targeted compilation and final assembly; behavioral criteria are encoded in source structure and runtime invariants but cannot be visually exercised by the implementation agent in this repository.

## Slices

### Slice 1 — Players and administrators can obtain and upgrade valid whips
**Status:** Complete

**Outcome**
A levelled whip exists as a safely identified custom lead. Players can craft and upgrade it through level V, automatic crafters follow the same rules, and authorized administrators can grant any level without affecting ordinary leads.

**Scope**
- Register `:feature:whip:paper` in `settings.gradle.kts` and create its Gradle project under `feature/whip/paper`.
- Add the `whip` Paper Runtime Module with a required `legacy_cloud_commands` capability and lifecycle-owned registrations.
- Add HOCON configuration and validated models for the five lengths and the combat, knockback, and sound tuning values required by later slices.
- Implement versioned item identity, level parsing, current-config item construction, unstackable lead presentation, and `Messages.kt` values with placeholders.
- Register the base and upgrade recipe discoverability/shape and implement exact preparation and consumption rules for inventory crafting, crafting tables, shift-click, and automatic crafters.
- Ensure only an ordinary lead can enter the base recipe, only a valid whip below level V can enter an upgrade, and outputs are rebuilt from current configuration.
- Implement `/whip give [player] [level]`, permission checks, defaults, argument validation, full-inventory refusal, sender/target feedback, and command cleanup.
- Remove owned recipes and listeners during module disable.

**Implementation notes**
- Use package `plutoproject.feature.whip.paper` and resource path `module/whip/config.conf`.
- Prefer public Paper API; do not select the dev-bundle convention unless compilation proves an accepted behavior unavailable in the API.
- Store only identity, schema version, and level in PDC. Do not use display name or lore as authority.
- Bukkit's static recipe ingredient model is insufficient to express “custom lead of level N” robustly across presentation changes. Follow the Gallery pattern: register recipe entries for discovery, but validate and materialize custom outputs in crafting listeners, including explicit automatic-crafter handling.
- Keep all dynamic command and item text as placeholder-based values in `Messages.kt`.

**Validation**
- `./gradlew :feature:whip:paper:compileKotlin :feature:whip:paper:processResources` — proves module registration, dependencies, command annotations, item/data-component usage, listener APIs, and configuration resources compile together. It does not run a Minecraft crafting UI or automatic crafter.
- `git diff --check` — proves the slice introduces no whitespace errors.

**Dependencies**
- None.

**Completed work**
- Added the Paper feature module, validated configuration, versioned lead identity, current-config presentation, recipes, exact crafting validation/consumption, automatic crafter handling, recipe discovery, `/whip give`, and disable-time cleanup.

**Validation**
- `./gradlew :feature:whip:paper:compileKotlin :feature:whip:paper:processResources` — passed with Java 25 configured via `JAVA_HOME`.
- `git diff --check` — passed.

**Deviations:** None.

### Slice 2 — Holding use deploys a terrain-aware physical whip
**Status:** Complete

**Outcome**
Using a whip from either hand shows one continuous vanilla-material chain anchored near that hand. It hangs, trails, collides with solid terrain, and retracts without leaking entities or tasks as soon as the item is no longer actively used.

**Scope**
- Add whip-only interaction handling that suppresses lead tether and knot behavior while preserving every ordinary-lead path.
- Configure the custom lead's continuous-use data component and trident use animation; detect the actual active hand rather than imposing main-hand state.
- Add a per-player session manager with one active session maximum and idempotent termination.
- Launch each session loop as a child of the module coroutine scope on `player.coroutineDispatcher`; run one iteration followed by cooperative `delay(50.milliseconds)` instead of registering a repeating scheduler task.
- Compute a hand-relative anchor and initialize a uniformly divided chain with maximum rest spacing of `0.5` blocks.
- Implement tick-rate Verlet/position-based integration, damping, gravity, iterative link constraints, and basic point resolution against solid block collision shapes.
- Spawn one oriented thin `BlockDisplay` per link, choose the best vanilla dark-brown material, distinguish the tip, and update transforms with Display interpolation.
- Before world interaction, calculate the square chunk radius covering configured length and sweep thickness and require current-region ownership of the full area; pause safely and reset motion history on the exceptional failed check.
- Handle release, item/hand change, drop, teleport/world change, death, quit, invalid player state, scheduler retirement, and module disable.
- Reset position and velocity history on session start and discontinuities so stale motion cannot feed the later combat calculation.

**Implementation notes**
- Separate item-use/session orchestration, chain simulation, terrain collision, and Display rendering so combat can consume simulation snapshots without depending on entities.
- Derive segment count as `ceil(length / 0.5)` and use `length / segmentCount` as the exact rest spacing.
- Treat Display entities only as interpolated views of adjacent simulation points. Disable their gravity and avoid marker/entity collision as a source of rope behavior.
- Use a small fixed number of constraint iterations and localized block-shape queries; do not scan unrelated chunks or world entities.
- A default 10-block whip needs only `Bukkit.isOwnedByCurrentRegion(player.location, 1)`; derive a larger radius when configuration exceeds that reach instead of assuming the default.
- Keep scheduler selection out of the Feature: the simulation loop and entity operations use `Entity.coroutineDispatcher`, whose foundation abstraction supplies the lower-latency server event-loop fallback on Paper and owning `EntityDispatcher` behavior on Folia. Localize and justify any other runtime-specific branch that proves unavoidable.
- Give every session an explicit child `Job`. Stop paths must remove the session from the registry, perform ownership-safe entity cleanup, then cancel and join the job as appropriate; the loop also uses `try/finally` as a backstop.
- Account for `EntityDispatcher`'s empty retired callback: quit/death/teleport and module shutdown handlers must stop sessions before their owner retires, rather than relying on a delayed continuation to observe invalidity.
- Make all cleanup safe to call repeatedly, keep cross-region registries concurrency-safe, and maintain a final module-level cleanup path independent of player events.

**Validation**
- `./gradlew :feature:whip:paper:compileKotlin` — proves item-use, event, collision-shape, transformation, Display, ownership-check, existing coroutine dispatcher, delay loop, and structured `Job` lifecycle APIs are valid for the pinned Paper version. Client interpolation, Folia ownership transitions, and visual naturalness cannot be observed without a running server/client.
- `git diff --check` — proves the slice introduces no whitespace errors.

**Dependencies**
- Slice 1.

**Completed work**
- Added identity-guarded custom whip interaction/use handling, trident animation data, and lifecycle termination listeners without changing ordinary lead behavior.
- Added one-session-per-player structured coroutine loops on `player.coroutineDispatcher`, including active-use validation, ownership-paused ticks, and module-safe concurrent shutdown tracking.
- Added uniformly segmented Verlet/position-based simulation with gravity, damping, constraints, hand anchoring, motion-history resets, and collision-shape terrain resolution.
- Added interpolated oriented vanilla-material `BlockDisplay` links with a thinner darker tip and dispatcher-routed idempotent cleanup.

**Validation**
- `JAVA_HOME=/home/nostalfinals/.local/share/mise/installs/java/25.0.2 PATH=/home/nostalfinals/.local/share/mise/installs/java/25.0.2/bin:$PATH ./gradlew :feature:whip:paper:compileKotlin` — passed.
- `git diff --check` — passed.
- No runtime server/client harness is available; visual behavior and live Paper/Folia ownership transitions were not observed.

**Deviations:** None.

### Slice 3 — Swept whip motion damages and knocks back multiple targets with audio feedback
**Status:** Complete

**Outcome**
Fast whip motion can strike every eligible living target swept in a tick. Damage rises with effective acceleration and distance from the handle without a Feature cap, while accepted hits receive bounded sweep-direction knockback and controlled vanilla sound feedback.

**Scope**
- Retain previous/current simulation snapshots and derive per-link velocity, gravity-compensated acceleration, normalized along-whip hit position, and instantaneous sweep direction.
- Build swept volumes for every moving link, query only nearby living candidates, and perform precise candidate intersection checks.
- Exclude the wielder, armor stands, dead entities, spectator players, and non-living entities.
- Unconditionally verify each candidate entity is owned by the current execution region before reading combat state or applying the same-tick settlement; do not wrap this check in Folia detection.
- Aggregate candidates by target for the tick, retaining the highest raw-damage candidate while still allowing any number of distinct targets.
- Implement the configured damage formula, reject non-finite results, and enforce per-wielder/per-target repeat intervals.
- Apply damage with the wielder attributed through Bukkit's standard event path and determine whether protection cancelled/rejected it before applying custom velocity.
- Apply acceleration/position-scaled knockback in the winning link's sweep direction, capped by configured maximum velocity increment.
- Add edge-triggered free-end crack audio and coalesced accepted-hit audio using configured vanilla sounds.
- Clear cooldown and sound-trigger state with its owning whip session and on module disable.

**Implementation notes**
- Collect and settle hits in the same simulation tick; aggregation must not schedule damage for the following tick.
- Broad-phase work should be proportional to the whip's swept bounds and nearby entities. Deduplicate candidates before narrow-phase checks where practical.
- Do not infer protection solely from target type. Preserve existing `EntityDamageByEntityEvent` integrations, including `pvp_toggle`, and apply no knockback when that damage path is cancelled.
- Compute knockback independently from uncapped damage so extreme damage cannot bypass the configured velocity cap.
- Reset acceleration history after discontinuities, missed simulation continuity, or a failed area-ownership check rather than interpreting them as a whip crack.

**Validation**
- `./gradlew :feature:whip:paper:compileKotlin` — proves swept-geometry, living-entity query, attributed damage, event observation, velocity, and sound APIs compile as one combat path. It cannot prove live interactions with third-party protection plugins without a server harness.
- `git diff --check` — proves the slice introduces no whitespace errors.

**Dependencies**
- Slice 2.

**Completed work**
- Added per-frame link velocities and gravity-compensated acceleration history with discontinuity resets.
- Added ownership-guarded swept link geometry, nearby living-target collection, same-tick per-target strongest-hit aggregation, cooldowns, attributed damage observation, and bounded directional knockback.
- Added edge-triggered crack audio and coalesced accepted-hit audio, with vanilla-only sound identifier validation and session cleanup.

**Validation**
- `JAVA_HOME=/home/nostalfinals/.local/share/mise/installs/java/25.0.2 PATH=/home/nostalfinals/.local/share/mise/installs/java/25.0.2/bin:$PATH ./gradlew :feature:whip:paper:compileKotlin` — passed.
- `./gradlew shadowJar` — passed.
- `git diff --check` — passed.
- No runtime server/protection-plugin harness is available; live combat, Folia ownership transitions, and third-party cancellation behavior were not observed.

**Deviations:** None.

## Final verification
- `./gradlew shadowJar` — builds the complete distribution, runs runtime-module indexing dependencies, and proves the new Feature integrates into the assembled plugin.
- `git diff --check` — verifies all implementation changes are free of whitespace errors.
- Inspect the final diff to confirm ordinary-lead event cancellation is always guarded by whip identity, every lifecycle registration has a matching disable cleanup path, simulation is a structured child coroutine on `Entity.coroutineDispatcher`, ownership checks are unconditional, no duplicated scheduler path exists, any runtime-specific branch is localized and justified, and cross-region cleanup switches to entity dispatchers; this is an agent-executable static review because no automated Minecraft runtime is present.
