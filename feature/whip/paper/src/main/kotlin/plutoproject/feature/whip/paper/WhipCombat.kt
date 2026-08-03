package plutoproject.feature.whip.paper

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.SoundCategory
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/** Observes the event result of the normal entity damage path used by the whip. */
internal class WhipDamageObserver : Listener {
    private val pendingAttempts = object : ThreadLocal<ArrayDeque<DamageAttempt>>() {
        override fun initialValue(): ArrayDeque<DamageAttempt> = ArrayDeque()
    }

    fun damage(target: LivingEntity, attacker: Player, amount: Double): Boolean {
        val attempt = DamageAttempt(target, attacker)
        val attempts = pendingAttempts.get()
        attempts.addLast(attempt)
        try {
            target.damage(amount, attacker)
        } catch (_: IllegalArgumentException) {
            return false
        } catch (_: IllegalStateException) {
            return false
        } finally {
            attempts.removeLast()
            if (attempts.isEmpty()) {
                pendingAttempts.remove()
            }
        }

        return attempt.observed &&
            !attempt.cancelled &&
            attempt.finalDamage.isFinite() &&
            attempt.finalDamage > 0.0
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val attempts = pendingAttempts.get()
        val attempt = attempts.peekLast() ?: return
        if (event.entity !== attempt.target || event.damager !== attempt.attacker) {
            return
        }

        attempt.observed = true
        attempt.cancelled = event.isCancelled
        attempt.finalDamage = event.finalDamage
    }

    private class DamageAttempt(
        val target: LivingEntity,
        val attacker: Player,
    ) {
        var observed = false
        var cancelled = false
        var finalDamage = 0.0
    }
}

/** Handles motion-derived damage, target settlement, knockback, and combat feedback. */
internal class WhipCombat(
    private val player: Player,
    private val config: WhipConfig,
    private val damageObserver: WhipDamageObserver,
) {
    private val hitCooldowns = HashMap<UUID, Long>()
    private var previousVelocities: List<Vector>? = null
    private var crackArmed = true
    private var simulationTick = 0L

    fun resetMotionHistory() {
        previousVelocities = null
        crackArmed = true
    }

    fun clear() {
        previousVelocities = null
        hitCooldowns.clear()
        crackArmed = true
        simulationTick = 0L
    }

    fun process(world: World, frame: WhipSimulationFrame) {
        val currentVelocities = frame.velocities
        val previous = previousVelocities
        previousVelocities = currentVelocities.map(Vector::clone)

        if (!frame.hasMotionHistory || previous == null || previous.size != currentVelocities.size) {
            crackArmed = true
            return
        }

        val pointAccelerations = currentVelocities.mapIndexed { index, velocity ->
            val previousVelocity = previous[index]
            velocity.clone()
                .subtract(previousVelocity)
                .subtract(GRAVITY_VECTOR(config.simulation.gravity))
        }
        playCrackSoundIfTriggered(world, frame, pointAccelerations)

        val sweeps = buildSweeps(frame, pointAccelerations)
        if (sweeps.isEmpty()) {
            simulationTick++
            return
        }

        val queryBounds = sweeps
            .map { it.bounds }
            .drop(1)
            .fold(sweeps.first().bounds.clone()) { bounds, sweepBounds ->
                bounds.union(sweepBounds)
            }
        val candidates = collectCandidates(world, queryBounds)
        if (candidates.isEmpty()) {
            simulationTick++
            return
        }

        val hits = LinkedHashMap<UUID, WhipHitCandidate>()
        for (sweep in sweeps) {
            for (target in candidates) {
                if (!Bukkit.isOwnedByCurrentRegion(target)) {
                    continue
                }
                if (!target.isValid || target.isDead) {
                    continue
                }

                val intersection = sweep.intersection(
                    targetBox = target.boundingBox,
                    thickness = config.simulation.sweepThickness,
                ) ?: continue
                val positionAlongWhip = (
                    sweep.index + intersection.linkProgress
                    ) / frame.current.lastIndex.toDouble()
                val rawDamage = (sweep.acceleration - config.combat.minAcceleration)
                    .coerceAtLeast(0.0) *
                    config.combat.damageScale *
                    positionAlongWhip * positionAlongWhip
                if (!rawDamage.isFinite() || rawDamage <= 0.0) {
                    continue
                }

                val candidate = WhipHitCandidate(
                    target = target,
                    targetId = target.uniqueId,
                    rawDamage = rawDamage,
                    acceleration = sweep.acceleration,
                    positionAlongWhip = positionAlongWhip,
                    impactPosition = intersection.position,
                    sweepDirection = sweep.direction,
                )
                val current = hits[candidate.targetId]
                if (current == null || candidate.rawDamage > current.rawDamage) {
                    hits[candidate.targetId] = candidate
                }
            }
        }

        settleHits(world, hits.values)
        simulationTick++
    }

    private fun collectCandidates(world: World, bounds: BoundingBox): List<LivingEntity> {
        val candidates = LinkedHashMap<UUID, LivingEntity>()
        for (entity in world.getNearbyEntities(bounds)) {
            if (!Bukkit.isOwnedByCurrentRegion(entity)) {
                continue
            }
            val living = entity as? LivingEntity ?: continue
            if (!Bukkit.isOwnedByCurrentRegion(living)) {
                continue
            }
            if (!living.isValid || living.isDead || living.uniqueId == player.uniqueId) {
                continue
            }
            if (living is ArmorStand) {
                continue
            }
            if (living is Player && living.gameMode == GameMode.SPECTATOR) {
                continue
            }
            candidates.putIfAbsent(living.uniqueId, living)
        }
        return candidates.values.toList()
    }

    private fun buildSweeps(
        frame: WhipSimulationFrame,
        pointAccelerations: List<Vector>,
    ): List<WhipLinkSweep> {
        if (config.combat.damageScale <= 0.0 || frame.current.size < 2) {
            return emptyList()
        }

        val sweeps = ArrayList<WhipLinkSweep>(frame.current.lastIndex)
        for (index in 0 until frame.current.lastIndex) {
            val acceleration = pointAccelerations[index]
                .clone()
                .add(pointAccelerations[index + 1])
                .multiply(0.5)
            if (!isFinite(acceleration)) {
                continue
            }
            val accelerationMagnitude = acceleration.length()
            if (!accelerationMagnitude.isFinite() ||
                accelerationMagnitude <= config.combat.minAcceleration
            ) {
                continue
            }

            val previousStart = frame.previous[index]
            val previousEnd = frame.previous[index + 1]
            val currentStart = frame.current[index]
            val currentEnd = frame.current[index + 1]
            if (!isFinite(previousStart) || !isFinite(previousEnd) ||
                !isFinite(currentStart) || !isFinite(currentEnd)
            ) {
                continue
            }

            val previousCenter = previousStart.clone().add(previousEnd).multiply(0.5)
            val currentCenter = currentStart.clone().add(currentEnd).multiply(0.5)
            val direction = normalizedOrNull(currentCenter.clone().subtract(previousCenter))
                ?: normalizedOrNull(currentEnd.clone().subtract(previousEnd))
                ?: normalizedOrNull(currentStart.clone().subtract(previousStart))
            val bounds = BoundingBox.of(previousStart, previousStart)
                .union(previousEnd)
                .union(currentStart)
                .union(currentEnd)
                .expand(config.simulation.sweepThickness / 2.0)
            sweeps += WhipLinkSweep(
                index = index,
                previousStart = previousStart.clone(),
                previousEnd = previousEnd.clone(),
                currentStart = currentStart.clone(),
                currentEnd = currentEnd.clone(),
                acceleration = accelerationMagnitude,
                direction = direction,
                bounds = bounds,
            )
        }
        return sweeps
    }

    private fun playCrackSoundIfTriggered(
        world: World,
        frame: WhipSimulationFrame,
        pointAccelerations: List<Vector>,
    ) {
        val freeEndAcceleration = pointAccelerations.lastOrNull()
            ?.takeIf(::isFinite)
            ?.length()
            ?.takeIf(Double::isFinite)
        if (freeEndAcceleration == null) {
            crackArmed = true
            return
        }

        val threshold = config.sounds.crackThreshold
        if (freeEndAcceleration < threshold) {
            crackArmed = true
            return
        }
        if (!crackArmed) {
            return
        }

        val freeEnd = frame.current.lastOrNull()?.takeIf(::isFinite) ?: return
        world.playSound(
            freeEnd.toLocation(world),
            config.sounds.crack.sound,
            SoundCategory.PLAYERS,
            config.sounds.crack.volume.toFloat(),
            config.sounds.crack.pitch.toFloat(),
        )
        crackArmed = false
    }

    private fun settleHits(world: World, hits: Collection<WhipHitCandidate>) {
        val feedbackPositions = ArrayList<Vector>()
        for (candidate in hits) {
            val nextAllowedTick = hitCooldowns[candidate.targetId]
            if (nextAllowedTick != null && simulationTick < nextAllowedTick) {
                continue
            }
            if (!Bukkit.isOwnedByCurrentRegion(candidate.target)) {
                continue
            }
            if (!candidate.target.isValid || candidate.target.isDead) {
                continue
            }

            hitCooldowns[candidate.targetId] = simulationTick +
                config.combat.hitIntervalTicks.toLong()
            if (!damageObserver.damage(candidate.target, player, candidate.rawDamage)) {
                continue
            }

            candidate.sweepDirection?.let { direction ->
                if (!Bukkit.isOwnedByCurrentRegion(candidate.target)) {
                    return@let
                }
                if (!candidate.target.isValid || candidate.target.isDead) {
                    return@let
                }
                val existingVelocity = candidate.target.velocity
                if (!isFinite(existingVelocity)) {
                    return@let
                }
                val incrementMagnitude = (
                    candidate.acceleration *
                        config.knockback.scale *
                        candidate.positionAlongWhip * candidate.positionAlongWhip
                    ).coerceAtMost(config.knockback.maxVelocityIncrement)
                if (!incrementMagnitude.isFinite() || incrementMagnitude <= 0.0) {
                    return@let
                }
                val newVelocity = existingVelocity.clone()
                    .add(direction.clone().multiply(incrementMagnitude))
                if (isFinite(newVelocity)) {
                    candidate.target.velocity = newVelocity
                }
            }
            feedbackPositions += candidate.impactPosition.clone()
        }

        playHitSounds(world, feedbackPositions)
    }

    private fun playHitSounds(world: World, positions: List<Vector>) {
        val emitted = ArrayList<Vector>()
        for (position in positions) {
            if (emitted.any { it.distanceSquared(position) <= HIT_SOUND_COALESCE_RADIUS_SQUARED }) {
                continue
            }
            world.playSound(
                position.toLocation(world),
                config.sounds.hit.sound,
                SoundCategory.PLAYERS,
                config.sounds.hit.volume.toFloat(),
                config.sounds.hit.pitch.toFloat(),
            )
            emitted += position
        }
    }

    private companion object {
        const val HIT_SOUND_COALESCE_RADIUS_SQUARED = 4.0

        fun GRAVITY_VECTOR(gravity: Double): Vector = Vector(0.0, -gravity, 0.0)
    }
}

private data class WhipHitCandidate(
    val target: LivingEntity,
    val targetId: UUID,
    val rawDamage: Double,
    val acceleration: Double,
    val positionAlongWhip: Double,
    val impactPosition: Vector,
    val sweepDirection: Vector?,
)

private data class WhipLinkSweep(
    val index: Int,
    val previousStart: Vector,
    val previousEnd: Vector,
    val currentStart: Vector,
    val currentEnd: Vector,
    val acceleration: Double,
    val direction: Vector?,
    val bounds: BoundingBox,
) {
    fun intersection(targetBox: BoundingBox, thickness: Double): WhipIntersection? {
        val expandedTargetBox = targetBox.clone().expand(thickness / 2.0)
        val maximumMotion = max(
            max(previousStart.distance(currentStart), previousEnd.distance(currentEnd)),
            max(
                previousStart.distance(currentEnd),
                previousEnd.distance(currentStart),
            ),
        )
        val sampleSpacing = max(thickness / 2.0, 0.05)
        val sampleCount = ceil(maximumMotion / sampleSpacing)
            .toInt()
            .coerceIn(2, MAX_SWEEP_SAMPLES)

        for (sample in 0..sampleCount) {
            val time = sample.toDouble() / sampleCount
            val start = interpolate(previousStart, currentStart, time)
            val end = interpolate(previousEnd, currentEnd, time)
            val hit = segmentIntersection(start, end, expandedTargetBox) ?: continue
            return WhipIntersection(
                linkProgress = projectProgress(hit.position, start, end),
                position = hit.position,
            )
        }

        val endpointPaths = listOf(
            previousStart to currentStart,
            previousEnd to currentEnd,
        )
        for ((pathStart, pathEnd) in endpointPaths) {
            val hit = segmentIntersection(pathStart, pathEnd, expandedTargetBox) ?: continue
            val linkStart = interpolate(previousStart, currentStart, hit.time)
            val linkEnd = interpolate(previousEnd, currentEnd, hit.time)
            return WhipIntersection(
                linkProgress = projectProgress(hit.position, linkStart, linkEnd),
                position = hit.position,
            )
        }
        return null
    }

    private companion object {
        const val MAX_SWEEP_SAMPLES = 64
    }
}

private data class WhipIntersection(
    val linkProgress: Double,
    val position: Vector,
)

private data class SegmentIntersection(
    val time: Double,
    val position: Vector,
)

private fun segmentIntersection(
    start: Vector,
    end: Vector,
    box: BoundingBox,
): SegmentIntersection? {
    val delta = end.clone().subtract(start)
    var entry = 0.0
    var exit = 1.0

    val origins = doubleArrayOf(start.x, start.y, start.z)
    val directions = doubleArrayOf(delta.x, delta.y, delta.z)
    val minimums = doubleArrayOf(box.minX, box.minY, box.minZ)
    val maximums = doubleArrayOf(box.maxX, box.maxY, box.maxZ)
    for (axis in origins.indices) {
        val origin = origins[axis]
        val direction = directions[axis]
        if (abs(direction) < 1.0E-12) {
            if (origin < minimums[axis] || origin > maximums[axis]) {
                return null
            }
            continue
        }

        var axisEntry = (minimums[axis] - origin) / direction
        var axisExit = (maximums[axis] - origin) / direction
        if (axisEntry > axisExit) {
            val swap = axisEntry
            axisEntry = axisExit
            axisExit = swap
        }
        entry = max(entry, axisEntry)
        exit = minOf(exit, axisExit)
        if (entry > exit) {
            return null
        }
    }

    if (exit < 0.0 || entry > 1.0) {
        return null
    }
    val hitTime = entry.coerceIn(0.0, 1.0)
    return SegmentIntersection(
        time = hitTime,
        position = start.clone().add(delta.multiply(hitTime)),
    )
}

private fun interpolate(start: Vector, end: Vector, time: Double): Vector =
    start.clone().add(end.clone().subtract(start).multiply(time))

private fun projectProgress(point: Vector, start: Vector, end: Vector): Double {
    val delta = end.clone().subtract(start)
    val lengthSquared = delta.lengthSquared()
    if (!lengthSquared.isFinite() || lengthSquared <= VECTOR_EPSILON_SQUARED) {
        return 0.0
    }
    return point.clone()
        .subtract(start)
        .dot(delta)
        .div(lengthSquared)
        .coerceIn(0.0, 1.0)
}

private fun normalizedOrNull(vector: Vector): Vector? {
    if (!isFinite(vector) || vector.lengthSquared() <= VECTOR_EPSILON_SQUARED) {
        return null
    }
    return vector.normalize()
}

private const val VECTOR_EPSILON_SQUARED = 1.0E-12

private fun isFinite(vector: Vector): Boolean =
    vector.x.isFinite() && vector.y.isFinite() && vector.z.isFinite()
