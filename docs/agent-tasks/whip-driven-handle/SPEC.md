# Specification: View-Driven Whip Handle

## Status
Approved for implementation.

## Context
The current whip has improved rendering, substep simulation, and terrain stability, but camera input is applied through a soft guide on the first free point no more than one physical link from the hand. Rotating the view therefore changes only a small target near the grip and does not give the flexible body enough boundary velocity or leverage to produce a usable cast.

Players must be able to deploy the whip, raise it with a continuous view movement, and immediately reverse or redirect that movement to send an inertial wave toward the tip. The existing visual polish and terrain behavior are valuable and must be retained.

## Goals and non-goals

### Goals
- Drive the flexible whip through a rigid handle whose tip moves along an arc when the player rotates their view.
- Make continuous gestures such as upper-right lift followed by lower-left sweep produce visible inertia and a physically propagated cast.
- Keep configured level length, visible presentation, and combat distance semantics based on the complete whip from grip to tip.
- Exclude the rigid handle from hit detection while preserving the existing flexible-body damage rules.
- Preserve the current substep, terrain, rendering, lifecycle, and ownership-safety improvements.

### Non-goals
- Holding the flexible body suspended along the aim direction after view movement stops.
- Applying an artificial directional force to the whole chain or directly steering the tip.
- A visually distinct handle material, color, or thickness treatment.
- Rigid-handle collision or damage.
- Packet-only rendering, additional client assets, or a combat balance pass.

## Required behavior

### Whip composition and length
- A deployed whip consists of a grip anchor, a rigid handle, and a flexible lash.
- Configured level length continues to mean total deployed length from grip anchor to free tip.
- `simulation.handle-length` configures the rigid handle length and defaults to `1.0` block.
- Flexible lash rest length is `totalLength - handleLength`.
- Handle length must be finite, positive, and strictly less than the shortest configured level length. Invalid combined configuration prevents the Feature from enabling with a configuration error.
- The existing `simulation.guide-strength` and `simulation.max-guide-pitch` settings are removed from the active configuration model and bundled configuration.

### Handle pose and view input
- The grip anchor remains the existing approximation of the active hand for the actual use hand.
- Handle direction uses the player's complete finite eye direction, including the full Minecraft pitch range. It is not clamped by the removed guide setting.
- Handle tip is `grip + normalizedViewDirection * handleLength`.
- The rigid handle follows the sampled player view directly; it has no soft guide or independent inertia. Inertia belongs to the flexible lash.
- Previous and current handle poses are interpolated across the existing two physical substeps. Grip position is interpolated linearly, while direction follows the shortest valid rotational path before the handle tip is derived, so a large view change drives an arc rather than merely a straight chord where feasible.
- Direction interpolation must remain finite and deterministic for near-parallel and near-opposite samples and across yaw wraparound.

### Flexible simulation
- The first flexible point is a hard kinematic boundary at the handle tip. All following points remain Verlet/PBD simulated with the current damping, gravity, distance constraints, substeps, and terrain collision behavior.
- The old compliant first-point guide constraint is removed.
- Movement of the handle tip must be retained as boundary-induced motion by the solver. Position or velocity history updates must not erase the correction that transfers tension into the flexible lash.
- A rapid view change is always valid casting input and never causes a motion reset merely because the handle tip traveled far.
- Discontinuity detection is based on abnormal grip-anchor translation, not handle-tip displacement or angular change. A true grip discontinuity rebuilds the lash from the current handle tip and direction and returns a frame without motion history.
- Initial and reset positions form an exact-length forward-drooping lash beginning at the current handle tip. Current and previous positions are identical so deployment and reset create no artificial velocity, hit, or crack.
- When view and grip input stop, the handle remains aimed but the flexible lash is free to settle and hang under gravity. No persistent aim-alignment force supports it.

### Rendering
- The visible trajectory runs continuously from grip through the rigid handle and flexible lash to the free tip.
- The rigid handle is subdivided for rendering at approximately the existing maximum segment spacing rather than represented by one visually long block.
- Rigid handle render points, including the handle tip boundary, remain collinear with the sampled handle pose and are not displaced by visual smoothing.
- Existing smoothing applies only within the flexible lash and continues to preserve terrain safety and the free endpoint.
- The rigid/flexible boundary has the same seam overlap and visual continuity as other adjacent segments.
- Existing brown vanilla materials, taper, thickness, BlockDisplay backend, normal entity tracking, and two-tick position/transformation interpolation remain in use. The handle receives no distinct visual styling.
- Display count is stable for a session and remains linear in total configured length.

### Combat and feedback
- Rigid handle segments are never included in broad-phase bounds, narrow-phase sweeps, hit candidates, or damage settlement.
- The flexible segment connecting the kinematic handle tip to the first free lash point is eligible for collision like every other flexible link.
- Hit position `p` remains normalized over total whip length. For an intersection at flexible distance `d` from the handle tip, `p = (handleLength + d) / totalLength`.
- Existing acceleration threshold, damage scale, `p²` weighting, strongest-hit aggregation, target cooldown, protection handling, and knockback rules remain unchanged.
- Crack detection continues to use only the flexible free tip.
- Frames without continuous motion history cannot hit targets or trigger a crack.

## Design decisions
- The canonical player input is a kinematic handle pose, not a soft direction hint applied to the rope.
- The flexible simulation starts at the handle tip. The grip-to-handle-tip section is visible but neither flexible nor damaging.
- Physical impulse is transferred through movement of the kinematic boundary and the existing constraints; no additional whole-chain input force is introduced in this iteration.
- A useful cast is a continuous gesture. If the player raises the view and then waits, the lash naturally drops rather than remaining staged in the air.
- Total configured length includes the handle so existing item lore, tier ranges, and area-ownership radius retain their meaning.
- The handle remains visually indistinguishable from the rest of the whip except for its rigid motion.

## Constraints
- Preserve the existing two-substep Verlet/PBD solver, swept point terrain collision, visual smoothing, segment overlap, taper, and synchronized Display interpolation unless an adaptation is strictly required at the rigid/flexible boundary.
- Preserve clone-safe simulation snapshots; deriving velocities must not mutate current or previous world positions.
- The session remains a child coroutine on `player.coroutineDispatcher`, and the existing area-ownership gate remains the sole prerequisite before world, block, combat, sound, or Display access for a tick.
- Display cleanup remains idempotent and routed through each Display entity's dispatcher.
- Physics, combat, and rendering remain separate responsibilities. Render-only handle subdivisions never become physical or combat points.
- Work remains linear in flexible point count, rigid render segment count, local collision shapes, and nearby combat candidates.
- Use public Paper APIs and vanilla assets without introducing protocol dependencies.

## Acceptance criteria
- With a stationary player, quickly moving the view toward the upper right and immediately toward the lower left moves the handle tip through the corresponding arc and sends a delayed wave through the flexible lash.
- Slow view movement pulls the lash without resetting it; stopping view movement allows the lash to settle under gravity.
- Full upward and downward pitch can orient the rigid handle, with no former 55-degree clamp.
- Rapid rotation, including crossing yaw wraparound, does not mark the frame discontinuous solely because of angular input.
- A true abnormal grip translation rebuilds the lash with no initial velocity and cannot create a hit or crack.
- The deployed whip's total grip-to-tip rest length equals its configured tier length, with a default 1.0-block rigid handle included in that total.
- Invalid handle length is rejected, and the old guide settings are no longer active configuration controls.
- The rendered whip remains continuous and uses the existing visual style; the handle stays straight while flexible smoothing begins after its tip.
- Rigid handle motion cannot damage a target, while the flexible lash can still damage multiple eligible targets using total-length-normalized hit position.
- Existing terrain behavior, visual interpolation, combat protection handling, session cleanup, and region ownership rules remain intact.
