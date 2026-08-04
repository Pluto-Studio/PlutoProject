# Implementation Plan: View-Driven Whip Handle

## References
- Specification: `SPEC.md`

## Progress
- [ ] Slice 1 — View rotation physically drives the flexible lash through a rigid handle
- [ ] Slice 2 — Rendering and combat honor the rigid/flexible boundary

## Current codebase state
- The working tree is clean before this task, and the completed `whip-feature` and `whip-motion-polish` task documents must remain unchanged.
- `WhipSessionManager.kt` currently samples a clamped guide direction and sends the hand anchor plus that direction to `WhipChain.step` once per 50 ms session iteration.
- `WhipChain.kt` currently treats configured tier length as one fully flexible chain rooted at the hand. Point zero is hard-anchored there, while `applyRootGuide` softly pulls point one toward a target no more than one rest spacing away.
- The chain already performs two anchor-interpolated substeps, scaled gravity/damping, distance constraints, swept point terrain collision, grip-length-relative discontinuity reset, and clone-safe outer-tick snapshots.
- `WhipRenderer.kt` currently receives only physical chain points, smooths every internal point, owns one normal world `BlockDisplay` per resulting link, and applies the completed centering, overlap, taper, terrain retraction, and two-tick interpolation behavior.
- `WhipCombat.kt` treats every physical link as damaging and computes hit position from frame index over `frame.current.lastIndex`. It has no separate total-length or rigid-handle boundary metadata.
- `WhipSimulationConfig` and bundled HOCON currently expose `guide-strength` and `max-guide-pitch`; they do not expose handle length.
- The default configured tier lengths are `4.0`, `5.5`, `7.0`, `8.5`, and `10.0`, so a default `1.0`-block handle leaves a positive flexible length for every tier.
- The module has no automated Minecraft runtime or client harness. Compilation and static invariant review cannot directly prove cast feel, arc appearance, or live hit exclusion.

## Slices

### Slice 1 — View rotation physically drives the flexible lash through a rigid handle
**Status:** Pending

**Outcome**
The player's complete view pose moves a one-block rigid handle tip, and that moving boundary pulls the flexible lash with preserved inertia. Continuous lift-and-sweep gestures drive the whip, while stopping input lets it hang naturally.

**Scope**
- Replace `guideStrength` and `maxGuidePitch` with validated `handleLength` in `WhipSimulationConfig` and bundled `module/whip/config.conf`.
- Add cross-field validation that handle length is finite, positive, and less than every configured total tier length.
- In `WhipSessionManager.kt`, retain the grip anchor and sample the complete normalized eye direction without pitch clamping.
- Derive previous/current handle poses and interpolate grip plus shortest-path direction across the existing two substeps before calculating each substep handle tip.
- Refactor `WhipChain` so its rest length is only `totalLength - handleLength` and its hard point-zero boundary is the handle tip.
- Remove the soft root guide and its per-iteration strength calculation.
- Track grip history separately from handle-tip/direction history: grip translation decides discontinuity, while angular handle motion remains valid input regardless of tip travel.
- Preserve boundary-induced corrections in Verlet history so handle-tip velocity and tension propagate into free points instead of being discarded.
- Initialize or reset an exact-length forward-drooping lash at the current handle tip with no motion history.

**Implementation notes**
- Keep total configured length available separately from flexible rest length for renderer and combat integration in Slice 2.
- Prefer direction-vector interpolation over yaw-number interpolation so crossing `-180°/180°` does not create a false long rotation. Use stable finite fallbacks for nearly parallel, opposite, or invalid samples.
- Compute each substep handle tip from its interpolated grip and direction, rather than linearly interpolating only between endpoint positions, so angular input preserves a sampled arc.
- Do not add direct acceleration to arbitrary free points. The moving kinematic boundary and PBD constraints are the only new drive mechanism.
- Do not weaken or bypass the existing area-ownership gate, terrain collision, damping, gravity, or substep count.

**Validation**
- `./gradlew :feature:whip:paper:compileKotlin :feature:whip:paper:processResources` — proves the replacement configuration, handle-pose sampling, refactored simulation API, and bundled defaults compile/package together; it cannot assess live casting feel.
- `git diff --check` — proves the slice introduces no whitespace errors.

**Dependencies**
- None.

### Slice 2 — Rendering and combat honor the rigid/flexible boundary
**Status:** Pending

**Outcome**
Nearby players still see one continuous, consistently styled whip from hand to tip, while only the flexible lash can hit targets and hit position retains its total-length meaning.

**Scope**
- Extend the simulation/session-to-render integration with the grip, handle pose, and flexible physical points needed to build a complete visual trajectory without exposing render subdivisions to combat.
- Subdivide the rigid handle for display at approximately the existing maximum render spacing.
- Keep all rigid handle points and the handle-tip boundary fixed during smoothing; apply existing bounded terrain-aware smoothing only to eligible flexible internal points.
- Preserve current materials, root-to-tip taper, segment overlap, centered transformations, two-tick interpolation, normal world tracking, and dispatcher-safe cleanup.
- Keep the Display list stable for the session and proportional to total length.
- Give combat enough distance metadata to iterate only flexible sweeps and calculate `p = (handleLength + flexibleDistance) / totalLength`.
- Preserve outer-tick physical velocity/acceleration, free-tip crack detection, strongest-candidate aggregation, cooldown, protection, damage, and knockback behavior.
- Verify no render-only rigid subdivisions can enter terrain simulation, acceleration history, hit bounds, or target intersection.

**Implementation notes**
- The flexible frame should begin at the kinematic handle tip so its first link can transfer motion and remain combat-eligible; the grip-to-tip rigid section is supplied separately to rendering.
- Render the handle with the same visual style rather than introducing a dedicated material or thickness branch. Any subdivision count must remain stable for fixed configuration.
- Preserve the handle tip exactly as a smoothing boundary. Smoothing across it would visually bend the rigid section and decouple the displayed drive point from physics.
- Derive flexible distance from exact flexible rest spacing plus intersection progress instead of assuming frame-index normalization still represents total length.
- Do not add handle collision, handle damage, a pose-hold force, or combat retuning.

**Validation**
- `./gradlew :feature:whip:paper:compileKotlin` — proves renderer/frame integration, Display APIs, combat distance mapping, and physical/render boundary types compile together; it cannot visually verify continuity or exercise live target sweeps.
- `git diff --check` — proves the slice introduces no whitespace errors.

**Dependencies**
- Slice 1.

## Final verification
- `./gradlew shadowJar` — assembles the complete plugin and proves the new task integrates with runtime-module indexing and the rest of the project.
- `git diff --check` — verifies all implementation and task-progress changes are free of whitespace errors.
- `git diff -- docs/agent-tasks/whip-feature docs/agent-tasks/whip-motion-polish` — must be empty, proving the two previous task artifacts were not overwritten.
- `git diff -- feature/whip/paper/src/main/kotlin/plutoproject/feature/whip/paper feature/whip/paper/src/main/resources/module/whip/config.conf` — statically review that full view rotation never triggers discontinuity by handle-tip distance, only flexible links enter combat, total-length hit progress is retained, visual smoothing preserves the rigid boundary, and existing ownership/cleanup paths remain in place.
- Confirm the final public simulation configuration contains `handle-length` and no active `guide-strength` or `max-guide-pitch` setting.
