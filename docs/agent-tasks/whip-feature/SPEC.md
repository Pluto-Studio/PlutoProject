# Specification: Whip Feature

## Status
Approved for implementation.

## Context
PlutoProject needs a Paper gameplay Feature for a craftable whip. The held item must remain recognizable to an unmodified Minecraft client as a vanilla lead, while holding use deploys a visible, physically responsive whip that can sweep through and damage multiple living targets.

The repository has no resource-pack infrastructure, so both the item and deployed whip must use vanilla client assets. Ordinary leads must retain all vanilla behavior.

## Goals and non-goals

### Goals
- Provide a five-level whip whose only level-dependent property is its configured length.
- Make the deployed whip visibly respond to gravity, terrain, wielder movement, and greater inertia toward its free end.
- Derive uncapped raw damage from simulated acceleration and the hit position along the whip.
- Detect every eligible living target swept by the whip rather than selecting a single target.
- Preserve standard server damage protections and provide physically intuitive, bounded knockback.
- Support survival crafting, upgrading, and an administrative give command.

### Non-goals
- A server resource pack, custom client model, or custom texture.
- Rope wrapping, knots, attachment to blocks, or special simulation for fluids, plants, and cobwebs.
- Vanilla lead tethering behavior for custom whip items.
- Particle effects, durability consumption, database persistence, or a configuration reload command.

## Required behavior

### Item identity and presentation
- A whip is a vanilla lead carrying namespaced custom data for its item type, data version, and level.
- Whip recognition must require that custom identity; ordinary leads and unrelated customized leads retain vanilla behavior.
- Whips are unstackable, have no durability, and show the localized name `皮鞭` plus lore containing their level and currently configured length.
- Runtime behavior derives length from the stored level and current configuration rather than persisting length in the item.
- The five valid levels are I through V. Their default lengths are `4.0`, `5.5`, `7.0`, `8.5`, and `10.0` blocks.
- Exactly five lengths are configurable. All must be finite, positive, and strictly increasing; invalid configuration must prevent the Feature from enabling with a clear configuration error.

### Acquisition and upgrades
- A shapeless recipe containing one ordinary lead and three leather creates a level-I whip.
- A shapeless recipe containing exactly one valid level-I through level-IV whip and one leather creates the next level while normalizing its item presentation to current configuration.
- A level-V whip has no upgrade result.
- Recipe validation must distinguish ordinary leads from whips and must not accept malformed or out-of-range whip data.
- Inventory crafting, crafting tables, shift-click crafting, and automatic crafters must apply equivalent ingredient validation and output rules without duplicating or losing items.
- Recipe registrations must be removed when the Feature disables.

### Administrative command
- Register `/whip give [player] [level]` through the existing legacy Cloud command capability.
- Require permission `plutoproject.whip.command.give`.
- Omitted `player` means the sending player; a non-player sender must specify an online target.
- Omitted `level` means level I. Explicit levels must be in the range I through V.
- Each invocation gives one whip. If the target inventory cannot accept it, no item is created or dropped and the sender receives a failure message.
- A successful grant notifies the sender. A distinct target is also notified.

### Activation and lifecycle
- A whip can be used from either hand. Minecraft's active-use hand identifies the deployed whip, and one player can have at most one deployed whip.
- Holding right click on a whip starts continuous item use with the vanilla trident use animation and deploys the simulated whip immediately.
- The whip remains deployed only while the player is actively using the same valid whip in the same hand.
- Custom whip interaction suppresses vanilla lead tethering, untethering, and lead-knot behavior. Checks and cancellation must not affect ordinary leads.
- Releasing use, changing or dropping the held item, changing the active hand, teleporting, changing world, dying, disconnecting, becoming invalid, or disabling the module retracts the whip and removes all associated tasks and display entities.
- Session creation or reset must initialize motion history so teleportation, scheduler delay, or stale state cannot become artificial acceleration or damage.
- Always drive each active session as a child coroutine of the module scope launched on the wielder through the project's entity-dispatcher abstraction, `player.coroutineDispatcher`. Run one simulation iteration per `50` milliseconds with cooperative `delay`, rather than registering a Bukkit/Paper repeating task.
- The session loop must use this single dispatcher path on every supported Paper-family runtime; it must not branch merely to choose a scheduler. `Entity.coroutineDispatcher` owns the Paper event-loop versus Folia `EntityDispatcher` selection.

### Physical simulation and rendering
- Simulate at the server tick rate using a segmented position-based/Verlet rope:
  - the first point is anchored to an approximation of the active hand derived from player position, view, and handed side;
  - free points integrate inertia, damping, and gravity;
  - iterative distance constraints preserve the configured total length;
  - farther points are free to accumulate visibly greater lag and inertia.
- Use enough uniformly spaced points that no link has a rest length greater than `0.5` blocks; the final spacing must divide the configured total length evenly.
- Before world access, unconditionally derive a conservative square chunk radius from configured whip length and sweep thickness and verify that the current execution context owns that entire area with `Bukkit.isOwnedByCurrentRegion`. Apply the same check on Paper and Folia rather than guarding it with runtime detection. The default maximum length of 10 blocks requires a one-chunk radius check.
- If that ownership check fails, perform no block, entity-query, combat, sound, spawn, or direct Display update for that tick; preserve or freeze visuals as safely possible and reset motion history so the skipped tick cannot create acceleration. Normal simulation resumes only after ownership is re-established.
- Resolve basic collisions against solid block collision shapes so points can rest on the ground, slide along walls, and be dragged over terrain. The solver does not wrap or attach the rope.
- Render each adjacent point pair as an oriented, thin `BlockDisplay` segment. Use vanilla dark-brown materials, with a slightly thinner or darker final section, choosing the vanilla material that gives the most continuous appearance.
- Use Display interpolation to smooth server-tick transforms. Display entities are visual only and must not be used as physics or hitboxes.
- The deployed whip is visible to nearby players through normal entity tracking.

### Swept hits and target selection
- For each tick, test the swept volume between every link's previous and current positions rather than only its final position.
- A sweep can collect any number of eligible targets along its path.
- Eligible targets are living, non-dead players and mobs, excluding the wielder, armor stands, and spectator players. Non-living entities are ignored.
- Defensively confirm `Bukkit.isOwnedByCurrentRegion(entity)` before reading or mutating every candidate, even after the enclosing area ownership check and regardless of server implementation.
- If multiple links or sweep portions hit the same target in one tick, retain only the candidate with the highest raw damage and settle it during that same tick.
- Track a per-wielder/per-target hit interval. The default interval is 10 ticks and is configurable as a positive integer.

### Damage
- For a hit candidate, compute motion-induced acceleration from the change in link velocity between simulation ticks after subtracting the configured gravity contribution.
- Let `p` be the normalized hit position along the whip, where the handle is `0` and the free end is `1`. Compute raw damage as:

  `max(0, acceleration - minAcceleration) * damageScale * p²`

- Defaults are `minAcceleration = 0.08` and `damageScale = 8.0`; both values are configurable and must be finite and non-negative.
- Length level supplies no direct damage multiplier.
- The Feature applies no upper damage cap. Non-finite computed values must be rejected rather than passed into the server API.
- Apply damage through the standard Bukkit damage path with the wielder as attacker, allowing armor, resistance, invulnerability frames, PvP Toggle, and other event-based protection to modify or cancel it.

### Knockback
- An accepted hit applies custom knockback in the normalized instantaneous sweep direction of the winning link, including its vertical component.
- Knockback magnitude grows with effective acceleration and `p²`, using a configurable non-negative scale, but is capped by a configurable maximum velocity increment.
- The default maximum velocity increment is `1.2` blocks per tick.
- No knockback is applied when damage is cancelled, rejected, or has no valid sweep direction.

### Audio feedback
- Use only vanilla sounds.
- Play a whip-crack sound near the free end when its effective acceleration crosses a configurable threshold from below. It cannot retrigger until acceleration falls below the threshold.
- Play hit feedback for accepted hits. Coalesce same-tick nearby hit sounds so a multi-target sweep does not create an excessive stack of identical sounds.
- Crack threshold, sound identifiers, volume, and pitch are configurable and must be validated.

### Messages
- All player-facing item, command, and error text is declared as values in `Messages.kt`.
- Dynamic level, length, player, and command values are inserted through placeholders rather than message-producing functions.

## Design decisions
- The Runtime Module and Gradle feature ID is `whip`, targeting Paper only and requiring `legacy_cloud_commands`.
- Physics, swept collision, combat, and rendering are separate responsibilities. Display entities never determine hits.
- Vanilla `BlockDisplay` segments are the initial renderer; no resource-pack integration is introduced.
- A same-tick per-target maximum avoids duplicate damage from segmentation without adding another tick of latency.
- Damage remains uncapped by this Feature, while knockback is capped independently for world and entity safety.
- Existing whip items are compatible with changed tier lengths because stored item data contains level, not physical length.
- The short whip range is handled as one wielder-dispatcher-owned area after an explicit radius ownership check; the Feature does not introduce asynchronous multi-region physics snapshots.
- Coroutine `Job` ownership is the task lifecycle mechanism. Per-session loops use structured concurrency under the module scope; Bukkit/Paper scheduled-task handles are not introduced.
- Folia compatibility is primarily an invariant of the common entity-dispatcher, ownership, concurrency, and lifecycle model rather than a parallel implementation. Runtime-specific branches remain permissible only where an API or behavior genuinely differs and the common abstraction cannot express it.

## Constraints
- Use Kotlin unless an API incompatibility requires Java.
- Player-facing text must follow the repository's `Messages.kt` convention.
- Every custom interaction path must identify the whip before cancelling vanilla behavior.
- Module disable and every session termination path must clean up displays, scheduled work, hit cooldown state, and sound threshold state.
- Display cleanup that may occur outside the owning region must switch to each Display entity's existing coroutine dispatcher; shared session registries and shutdown coordination must be safe under concurrent region threads.
- No world, block, or entity state may be accessed merely because it is within view distance; current-region ownership is the authority.
- Do not scatter runtime checks or maintain separate Paper and Folia implementations. Any unavoidable runtime-specific branch must be localized at the differing integration boundary and document why the shared dispatcher/concurrency model is insufficient.
- The implementation must remain bounded by configured whip length: simulation points, displays, terrain checks, and swept-hit work scale linearly with segment count and nearby candidates rather than scanning all world entities.

## Acceptance criteria
- A normal lead still crafts and tethers entities exactly as before, while a marked whip cannot tether entities or create lead knots.
- Players can craft a level-I whip, upgrade it one level at a time through level V, and cannot upgrade level V; the same rules hold for automatic crafters.
- An authorized sender can grant any valid level with `/whip give`, while invalid levels and full inventories create no item.
- Either hand can deploy one whip by holding use, showing the trident use pose and a continuous vanilla-material whip from the corresponding hand.
- The deployed chain visibly hangs under gravity, lags more toward its tip, rests or slides on solid terrain, and is fully removed on every termination condition.
- One sweep can damage multiple eligible living entities along its traveled path.
- Multiple same-tick link contacts with one target produce one damage settlement using the strongest candidate, and the configured repeat interval is enforced independently per target.
- Faster acceleration and hits nearer the free end produce greater raw damage, with no Feature-level maximum.
- Accepted hits knock targets in the whip's sweep direction without exceeding configured knockback maximum; cancelled damage produces no knockback.
- Crack and hit sounds use vanilla assets and do not retrigger or stack every tick.
- Changing a configured tier length changes the simulated length of existing valid whips at that level after module reload or server restart.
- Disabling the Feature leaves no recipes, commands, active tasks, or whip display entities behind.
- The same implementation runs through `Entity.coroutineDispatcher` on Paper and Folia: verified owned areas simulate normally, an ownership failure causes a safe non-damaging paused iteration, and cleanup performs no cross-region entity access.
