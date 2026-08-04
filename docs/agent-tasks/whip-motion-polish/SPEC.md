# Specification: Whip Motion and Rendering Polish

## Status
Approved for implementation.

## Context
The deployed whip now exists at the correct world position and can hit targets, but its current presentation and motion expose several defects: links can appear offset or disconnected, the chain often hangs almost vertically from the hand, visual positions advance in server-tick steps, fast view rotation does not drive a convincing wave toward the tip, and endpoint-only terrain resolution can allow visible penetration.

The affected users are players wielding a whip and nearby players observing it. Combat already consumes simulation snapshots independently from Display entities and must retain that separation.

## Goals and non-goals

### Goals
- Make a newly deployed or resting whip extend in front of the wielder before hanging naturally under gravity.
- Transfer rapid view rotation into delayed motion along the chain without turning the root into a rigid rod.
- Improve terrain stability during fast movement without changing the server tick rate seen by combat.
- Render a continuous, tapered whip with smooth client-side motion and a stable Display count.
- Preserve existing combat semantics, lifecycle cleanup, region ownership safety, and vanilla-client compatibility.

### Non-goals
- Packet-only or per-viewer virtual entity rendering.
- A resource pack, custom model, texture, or non-vanilla client requirement.
- Full link-capsule terrain collision, rope wrapping, knots, or attachment to terrain.
- More render segments than physical links.
- Damage, knockback, cooldown, target-selection, or audio rebalance.

## Required behavior

### View-driven physical shape
- Point zero remains a hard hand anchor.
- The first free point is pulled toward one rest-length step along a guide direction by a compliant constraint. It must retain enough freedom for gravity and chain inertia to affect the root instead of behaving as a second hard anchor.
- The guide direction follows player yaw fully. Its pitch is clamped symmetrically by `simulation.max-guide-pitch` before use, so looking steeply up or down cannot make the root nearly vertical.
- `simulation.guide-strength` controls the compliant direction constraint. It must be finite and in the inclusive range `0.0..1.0`.
- `simulation.max-guide-pitch` is expressed in degrees. It must be finite and in the inclusive range `0.0..89.0`.
- Defaults are `guide-strength = 0.35` and `max-guide-pitch = 55.0`.
- Initial chain positions form a deterministic forward-drooping curve: the root begins along the clamped guide direction and successive links progressively gain downward slope. Initialization must preserve the configured total length and must not create initial velocity.
- Rapid yaw or allowed pitch changes feed through the compliant root constraint and propagate toward the free end through the existing distance constraints.

### Continuity and discontinuities
- The simulation records the preceding hand anchor and interpolates from it to the current anchor across internal substeps.
- An anchor displacement that is too large to represent ordinary continuous movement is a discontinuity. Its threshold is derived from chain length rather than exposed as another tuning option.
- On a discontinuity, the chain is rebuilt at the current anchor using the forward-drooping initial shape. The resulting frame has no motion history, so combat and crack detection cannot interpret the reset as acceleration.
- Session start, region-ownership pauses, and existing lifecycle resets retain the same no-history guarantee.

### Substep simulation and terrain
- Each server tick executes exactly two internal physical substeps while still producing one outer `WhipSimulationFrame`.
- Gravity and damping are scaled for the half-tick steps so existing configuration retains its per-server-tick meaning. Adding substeps must not intentionally double gravity or damping.
- The hand anchor is interpolated for each substep. Distance constraints and terrain resolution run within each substep, with point zero restored to that substep's anchor as necessary.
- Every free point performs swept terrain collision from its prior substep position to its proposed position, using the configured simulation thickness as its collision radius. Collision must use solid block collision shapes and continue to ignore liquids.
- Constraint correction and contact restoration must be ordered or repeated so length solving does not leave resolved points embedded in terrain.
- The solver remains bounded by chain point count and the collision shapes in the points' local swept bounds. It does not perform full capsule tests for every link.
- The outer frame's `previous`, `current`, and velocity values describe the complete server tick. Intermediate substep acceleration spikes are not exposed to combat.

### Visual trajectory
- Rendering consumes simulation positions but may derive a separate visual trajectory. Combat, collision, sounds, and hit sweeps never consume visual positions.
- A single bounded smoothing pass preserves the first and last point, retains the original point count, and limits each internal point's displacement relative to its adjacent physical links.
- Visual smoothing must not knowingly move a point into a solid collision shape. When a smoothed point is unsafe, the renderer falls back toward the corresponding physical point rather than accepting deeper terrain penetration.
- One `BlockDisplay` represents each physical link. Display count remains constant for a session unless the underlying chain size changes, and the renderer must not churn entities merely because the visual curve changes.

### Segment geometry and interpolation
- Each Display's transformed vanilla block model must span its intended adjacent visual points around a correctly calculated origin. The block model's native `0..1` bounds must not offset a link beyond those points.
- Adjacent rendered links overlap by a small fixed amount along their axes to hide interpolation and corner seams without materially changing apparent whip length.
- Thickness decreases continuously from root to tip. The body remains a thin vanilla dark-brown material, with any darker tip treatment remaining visually compatible.
- Entity-position interpolation and transformation interpolation use the same two-tick duration. A segment's origin, orientation, length, and thickness therefore move as one visual unit rather than combining an instant teleport with a delayed transform.
- Displays remain non-persistent, visual-only world entities visible through normal server tracking. Existing ownership-safe and idempotent cleanup behavior remains intact.

### Combat invariants
- Damage processing continues to use the unsmoothed outer physical frame at one frame per server tick.
- Existing acceleration formula, gravity compensation, hit interval, target aggregation, damage scale, knockback calculation, and sound thresholds remain unchanged.
- No frame marked as lacking motion history may settle hits or trigger a crack.

## Design decisions
- Normal world `BlockDisplay` entities remain the rendering backend. Packet-level virtual entities are deferred unless future measured entity-tracking cost justifies a separate renderer replacement.
- Physical and visual trajectories are deliberately distinct: constrained visual smoothing can improve continuity but is never authoritative for world interaction.
- The first free point uses a soft directional constraint; two hard root points are rejected because they would create a rigid handle and amplify abrupt camera input.
- Two fixed substeps plus swept point collision provide the intended stability/performance balance. Full per-link capsule collision is deferred.
- Smoothing reuses the physical segment count. Visual quality must not be obtained by doubling Display entities.
- Client interpolation is fixed at two ticks for this iteration. Longer server-side visual history is rejected to avoid additional input lag.
- Only guide strength and maximum guide pitch are new configuration surface. Substep count, interpolation duration, smoothing bounds, overlap, and taper remain coherent internal constants for this iteration.

## Constraints
- Preserve the existing clone-before-subtract fix when generating frame velocities; snapshot vectors must never be mutated while deriving velocity.
- Preserve the one-session-per-player coroutine lifecycle on `player.coroutineDispatcher` and the existing area-ownership gate before all world access.
- Terrain checks and visual collision safety checks occur only while the current execution context owns the required area.
- Display removal continues through each Display entity's dispatcher and remains safe to invoke repeatedly.
- Runtime work and world entity count scale linearly with physical segment count and local collision shapes. At default maximum length, rendering remains limited to the existing 20 Displays per active whip.
- The implementation must use public Paper APIs and vanilla client assets; no protocol-library dependency is introduced.

## Acceptance criteria
- Deploying a whip while looking at an ordinary angle produces a chain that initially extends forward and then droops instead of forming a straight vertical column under the hand.
- Fast view rotation visibly drives a delayed wave toward the tip, while a stationary view still allows the chain to hang under gravity.
- Looking steeply up or down cannot force the root beyond the configured maximum guide pitch.
- Invalid guide strength or guide pitch configuration prevents the Feature from enabling with a configuration validation error.
- Ordinary movement remains continuous across substeps; an exceptional anchor jump rebuilds the chain and cannot create a hit or crack from stale motion.
- Fast point movement is checked along its traveled path against solid block collision shapes, and constraint solving restores contacts rather than knowingly leaving points embedded.
- Rendered links align with their intended endpoints, overlap enough to avoid obvious gaps, and taper progressively without increasing the Display count.
- Visual updates interpolate position and model transformation together over two ticks and do not exhibit the previous center-position jump combined with delayed scaling.
- Visual smoothing preserves endpoints and segment count and falls back when smoothing would introduce solid-terrain penetration.
- Existing target selection, damage, knockback, cooldown, sounds, session cleanup, and region-ownership behavior remain functionally unchanged.
