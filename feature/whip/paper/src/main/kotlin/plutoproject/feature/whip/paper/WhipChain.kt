package plutoproject.feature.whip.paper

import org.bukkit.World
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal data class WhipSimulationFrame(
    val grip: Vector,
    val previous: List<Vector>,
    val current: List<Vector>,
    val velocities: List<Vector>,
    val totalLength: Double,
    val handleLength: Double,
    val flexibleSegmentLength: Double,
    val hasMotionHistory: Boolean,
)

/**
 * A small position-based rope. The renderer and later combat code consume only its snapshots;
 * neither of them needs to know how the points are integrated.
 */
internal class WhipChain(
    val totalLength: Double,
    val handleLength: Double,
) {
    val flexibleLength: Double = totalLength - handleLength
    val segmentCount: Int = ceil(flexibleLength / MAX_REST_SPACING).toInt().coerceAtLeast(1)
    private val spacing: Double = flexibleLength / segmentCount
    private val discontinuityThreshold: Double = totalLength * DISCONTINUITY_LENGTH_FRACTION
    private val positions = MutableList(segmentCount + 1) { Vector() }
    private val verletPrevious = MutableList(segmentCount + 1) { Vector() }
    private val substepPrevious = MutableList(segmentCount + 1) { Vector() }
    private var previousGrip: Vector? = null
    private var previousDirection: Vector? = null
    private var initialized = false

    fun resetMotionHistory() {
        initialized = false
        previousGrip = null
        previousDirection = null
    }

    fun step(
        grip: Vector,
        direction: Vector,
        world: World,
        simulation: WhipSimulationConfig,
    ): WhipSimulationFrame {
        val currentDirection = normalizedDirection(direction)
        val startGrip = previousGrip?.clone() ?: grip.clone()
        val startDirection = previousDirection?.clone() ?: currentDirection.clone()
        if (!initialized || isDiscontinuous(grip)) {
            val handleTip = handleTip(grip, currentDirection)
            initialize(handleTip, currentDirection)
            previousGrip = grip.clone()
            previousDirection = currentDirection.clone()
            return stationaryFrame(grip)
        }

        val framePrevious = positions.map(Vector::clone)
        prepareSubstepHistory()
        val substepDamping = sqrt(simulation.damping)
        val substepGravity = simulation.gravity * SUBSTEP_DURATION * SUBSTEP_DURATION

        for (substep in 1..SUBSTEP_COUNT) {
            val progress = substep.toDouble() / SUBSTEP_COUNT
            val substepGrip = interpolate(startGrip, grip, progress)
            val substepDirection = interpolateDirection(startDirection, currentDirection, progress)
            val substepHandleTip = handleTip(substepGrip, substepDirection)
            positions[0].copy(substepHandleTip)
            integrateFreePoints(substepDamping, substepGravity)

            repeat(simulation.constraintIterations) {
                positions[0].copy(substepHandleTip)
                enforceLinkConstraints()
                positions[0].copy(substepHandleTip)
                for (index in 1 until positions.size) {
                    TerrainCollision.resolve(
                        position = positions[index],
                        previous = substepPrevious[index],
                        world = world,
                        radius = simulation.sweepThickness / 2.0,
                    )
                }
            }
            positions[0].copy(substepHandleTip)
        }

        positions[0].copy(handleTip(grip, currentDirection))
        verletPrevious.zip(framePrevious).forEach { (previous, frameStart) -> previous.copy(frameStart) }
        previousGrip = grip.clone()
        previousDirection = currentDirection.clone()

        val frameCurrent = positions.map(Vector::clone)
        val velocities = frameCurrent.zip(framePrevious) { current, previous ->
            current.clone().subtract(previous)
        }
        return WhipSimulationFrame(
            grip = grip.clone(),
            previous = framePrevious,
            current = frameCurrent,
            velocities = velocities,
            totalLength = totalLength,
            handleLength = handleLength,
            flexibleSegmentLength = spacing,
            hasMotionHistory = true,
        )
    }

    private fun stationaryFrame(grip: Vector): WhipSimulationFrame {
        val snapshot = positions.map(Vector::clone)
        return WhipSimulationFrame(
            grip = grip.clone(),
            previous = snapshot.map(Vector::clone),
            current = snapshot,
            velocities = List(snapshot.size) { Vector() },
            totalLength = totalLength,
            handleLength = handleLength,
            flexibleSegmentLength = spacing,
            hasMotionHistory = false,
        )
    }

    private fun prepareSubstepHistory() {
        for (index in 1 until positions.size) {
            val fullTickVelocity = positions[index].clone().subtract(verletPrevious[index])
            substepPrevious[index].copy(positions[index]).subtract(
                fullTickVelocity.multiply(SUBSTEP_DURATION),
            )
        }
    }

    private fun integrateFreePoints(damping: Double, gravity: Double) {
        for (index in 1 until positions.size) {
            val position = positions[index]
            val previous = substepPrevious[index]
            val velocity = position.clone().subtract(previous).multiply(damping)
            previous.copy(position)
            position.add(velocity)
            position.setY(position.y - gravity)
        }
    }

    private fun isDiscontinuous(grip: Vector): Boolean {
        val previous = previousGrip ?: return false
        return previous.distanceSquared(grip) > discontinuityThreshold * discontinuityThreshold
    }

    private fun initialize(handleTip: Vector, direction: Vector) {
        positions[0].copy(handleTip)
        verletPrevious[0].copy(handleTip)
        substepPrevious[0].copy(handleTip)

        var point = handleTip.clone()
        for (index in 1 until positions.size) {
            val droopProgress = (index - 1).toDouble() / (segmentCount - 1).coerceAtLeast(1)
            val segmentDirection = interpolateDirection(
                direction,
                DOWNWARD_DIRECTION,
                INITIAL_TIP_DROOP * droopProgress,
            )
            point.add(segmentDirection.multiply(spacing))
            positions[index].copy(point)
            verletPrevious[index].copy(point)
            substepPrevious[index].copy(point)
        }
        initialized = true
    }

    private fun handleTip(grip: Vector, direction: Vector): Vector = grip.clone().add(
        direction.clone().multiply(handleLength),
    )

    private fun normalizedDirection(direction: Vector): Vector {
        val normalized = direction.clone()
        return if (!isFinite(normalized) || normalized.lengthSquared() < DIRECTION_EPSILON) {
            DEFAULT_DIRECTION.clone()
        } else {
            normalized.normalize()
        }
    }

    private fun interpolateDirection(start: Vector, end: Vector, progress: Double): Vector {
        if (progress <= 0.0) {
            return normalizedDirection(start)
        }
        if (progress >= 1.0) {
            return normalizedDirection(end)
        }

        val from = normalizedDirection(start)
        val to = normalizedDirection(end)
        val dot = from.dot(to).coerceIn(-1.0, 1.0)
        if (dot > PARALLEL_DIRECTION_DOT) {
            return normalizedDirection(
                from.multiply(1.0 - progress).add(to.multiply(progress)),
            )
        }

        val angle = acos(dot)
        var tangent = to.clone().subtract(from.clone().multiply(dot))
        if (!isFinite(tangent) || tangent.lengthSquared() < DIRECTION_EPSILON) {
            tangent = deterministicPerpendicular(from)
        } else {
            tangent.normalize()
        }
        return normalizedDirection(
            from.multiply(cos(angle * progress))
                .add(tangent.multiply(sin(angle * progress))),
        )
    }

    private fun deterministicPerpendicular(direction: Vector): Vector {
        val basis = when {
            abs(direction.x) <= abs(direction.y) && abs(direction.x) <= abs(direction.z) ->
                Vector(1.0, 0.0, 0.0)
            abs(direction.y) <= abs(direction.z) -> Vector(0.0, 1.0, 0.0)
            else -> Vector(0.0, 0.0, 1.0)
        }
        return basis.subtract(direction.clone().multiply(basis.dot(direction))).normalize()
    }

    private fun isFinite(vector: Vector): Boolean =
        vector.x.isFinite() && vector.y.isFinite() && vector.z.isFinite()

    private fun enforceLinkConstraints() {
        for (index in 1 until positions.size) {
            val previousPoint = positions[index - 1]
            val point = positions[index]
            val delta = point.clone().subtract(previousPoint)
            val distance = delta.length()
            if (distance < DIRECTION_EPSILON) {
                point.copy(previousPoint).add(Vector(0.0, -spacing, 0.0))
                continue
            }

            val correction = delta.multiply((distance - spacing) / distance)
            if (index == 1) {
                point.subtract(correction)
            } else {
                val halfCorrection = correction.multiply(0.5)
                previousPoint.add(halfCorrection)
                point.subtract(halfCorrection)
            }
        }
    }

    private fun interpolate(start: Vector, end: Vector, progress: Double): Vector = start.clone().add(
        end.clone().subtract(start).multiply(progress),
    )

    private companion object {
        const val MAX_REST_SPACING = 0.5
        const val DISCONTINUITY_LENGTH_FRACTION = 0.5
        const val INITIAL_TIP_DROOP = 0.65
        const val SUBSTEP_COUNT = 2
        const val SUBSTEP_DURATION = 1.0 / SUBSTEP_COUNT
        const val DIRECTION_EPSILON = 1.0E-8
        const val PARALLEL_DIRECTION_DOT = 0.9995
        val DEFAULT_DIRECTION = Vector(0.0, 0.0, 1.0)
        val DOWNWARD_DIRECTION = Vector(0.0, -1.0, 0.0)
    }
}

private object TerrainCollision {
    private const val MAX_RESOLUTION_PASSES = 3
    private const val SEPARATION_EPSILON = 1.0E-4
    private const val SWEEP_EPSILON = 1.0E-8

    fun resolve(
        position: Vector,
        previous: Vector,
        world: World,
        radius: Double,
    ) {
        if (!position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()) {
            return
        }

        val sweepStart = previous.clone()
        val collisionBoxes = collisionBoxes(world, sweptBounds(sweepStart, position, radius))
        findSweepContact(sweepStart, position, collisionBoxes, radius)?.let { contact ->
            val velocity = position.clone().subtract(sweepStart)
            position.copy(sweepStart).add(velocity.multiply(contact.time))
            position.add(contact.normal.clone().multiply(SEPARATION_EPSILON))
            preserveSlideVelocity(position, previous, velocity, contact.normal)
        }

        repeat(MAX_RESOLUTION_PASSES) {
            val pointBounds = BoundingBox.of(position, radius, radius, radius)
            val contact = collisionBoxes.asSequence()
                .filter { it.overlaps(pointBounds) }
                .map { blockBox -> CollisionContact.from(position, blockBox, pointBounds, radius) }
                .minByOrNull(CollisionContact::penetration)
                ?: return
            val velocity = position.clone().subtract(previous)
            contact.resolve(position)
            preserveSlideVelocity(position, previous, velocity, contact.normal)
        }
    }

    private fun collisionBoxes(world: World, bounds: BoundingBox): List<BoundingBox> {
        val minBlockX = floor(bounds.minX).toInt()
        val maxBlockX = floor(bounds.maxX).toInt()
        val minBlockY = floor(bounds.minY).toInt().coerceAtLeast(world.minHeight)
        val maxBlockY = floor(bounds.maxY).toInt().coerceAtMost(world.maxHeight - 1)
        val minBlockZ = floor(bounds.minZ).toInt()
        val maxBlockZ = floor(bounds.maxZ).toInt()
        if (minBlockY > maxBlockY) {
            return emptyList()
        }

        val boxes = ArrayList<BoundingBox>()
        for (blockX in minBlockX..maxBlockX) {
            for (blockY in minBlockY..maxBlockY) {
                for (blockZ in minBlockZ..maxBlockZ) {
                    val block = world.getBlockAt(blockX, blockY, blockZ)
                    if (!block.isCollidable || block.isLiquid) {
                        continue
                    }
                    block.collisionShape.boundingBoxes.forEach { relativeBox ->
                        boxes += BoundingBox(
                            relativeBox.minX + blockX,
                            relativeBox.minY + blockY,
                            relativeBox.minZ + blockZ,
                            relativeBox.maxX + blockX,
                            relativeBox.maxY + blockY,
                            relativeBox.maxZ + blockZ,
                        )
                    }
                }
            }
        }
        return boxes
    }

    private fun sweptBounds(start: Vector, end: Vector, radius: Double): BoundingBox = BoundingBox(
        min(start.x, end.x) - radius,
        min(start.y, end.y) - radius,
        min(start.z, end.z) - radius,
        maxOf(start.x, end.x) + radius,
        maxOf(start.y, end.y) + radius,
        maxOf(start.z, end.z) + radius,
    )

    private fun findSweepContact(
        start: Vector,
        end: Vector,
        collisionBoxes: List<BoundingBox>,
        radius: Double,
    ): SweepContact? {
        val displacement = end.clone().subtract(start)
        return collisionBoxes.asSequence()
            .filterNot { isInsideExpandedBox(start, it, radius) }
            .mapNotNull { box -> sweptContact(start, displacement, box, radius) }
            .filter { it.time in 0.0..1.0 }
            .minByOrNull(SweepContact::time)
    }

    private fun isInsideExpandedBox(point: Vector, box: BoundingBox, radius: Double): Boolean =
        point.x >= box.minX - radius && point.x <= box.maxX + radius &&
            point.y >= box.minY - radius && point.y <= box.maxY + radius &&
            point.z >= box.minZ - radius && point.z <= box.maxZ + radius

    private fun sweptContact(
        start: Vector,
        displacement: Vector,
        box: BoundingBox,
        radius: Double,
    ): SweepContact? {
        var entryTime = 0.0
        var exitTime = 1.0
        var entryNormal: Vector? = null

        fun clipAxis(startValue: Double, delta: Double, minimum: Double, maximum: Double, axis: Int): Boolean {
            if (abs(delta) < SWEEP_EPSILON) {
                return startValue in minimum..maximum
            }
            val firstTime = (minimum - startValue) / delta
            val secondTime = (maximum - startValue) / delta
            val axisEntry = min(firstTime, secondTime)
            val axisExit = maxOf(firstTime, secondTime)
            if (axisEntry > entryTime) {
                entryTime = axisEntry
                entryNormal = axisNormal(axis, if (delta > 0.0) -1.0 else 1.0)
            }
            exitTime = min(exitTime, axisExit)
            return entryTime <= exitTime
        }

        if (!clipAxis(start.x, displacement.x, box.minX - radius, box.maxX + radius, 0) ||
            !clipAxis(start.y, displacement.y, box.minY - radius, box.maxY + radius, 1) ||
            !clipAxis(start.z, displacement.z, box.minZ - radius, box.maxZ + radius, 2)
        ) {
            return null
        }
        return entryNormal?.let { SweepContact(entryTime, it) }
    }

    private fun axisNormal(axis: Int, direction: Double): Vector = when (axis) {
        0 -> Vector(direction, 0.0, 0.0)
        1 -> Vector(0.0, direction, 0.0)
        else -> Vector(0.0, 0.0, direction)
    }

    private fun preserveSlideVelocity(
        position: Vector,
        previous: Vector,
        velocity: Vector,
        normal: Vector,
    ) {
        val normalVelocity = velocity.dot(normal)
        val slideVelocity = if (normalVelocity < 0.0) {
            velocity.subtract(normal.clone().multiply(normalVelocity))
        } else {
            velocity
        }
        previous.copy(position).subtract(slideVelocity)
    }

    private data class SweepContact(
        val time: Double,
        val normal: Vector,
    )

    private data class CollisionContact(
        val normal: Vector,
        val penetration: Double,
        val resolve: (Vector) -> Unit,
    ) {
        companion object {
            fun from(
                position: Vector,
                blockBox: BoundingBox,
                pointBounds: BoundingBox,
                radius: Double,
            ): CollisionContact {
                val overlapX = min(pointBounds.maxX, blockBox.maxX) -
                    maxOf(pointBounds.minX, blockBox.minX)
                val overlapY = min(pointBounds.maxY, blockBox.maxY) -
                    maxOf(pointBounds.minY, blockBox.minY)
                val overlapZ = min(pointBounds.maxZ, blockBox.maxZ) -
                    maxOf(pointBounds.minZ, blockBox.minZ)
                val penetration = minOf(overlapX, overlapY, overlapZ)
                return when (penetration) {
                    overlapX -> axisContact(
                        position.x,
                        blockBox.minX,
                        blockBox.maxX,
                        radius,
                        axis = 0,
                    )

                    overlapY -> axisContact(
                        position.y,
                        blockBox.minY,
                        blockBox.maxY,
                        radius,
                        axis = 1,
                    )

                    else -> axisContact(
                        position.z,
                        blockBox.minZ,
                        blockBox.maxZ,
                        radius,
                        axis = 2,
                    )
                }
            }

            private fun axisContact(
                coordinate: Double,
                minimum: Double,
                maximum: Double,
                radius: Double,
                axis: Int,
            ): CollisionContact {
                val towardPositive = coordinate >= (minimum + maximum) / 2.0
                val normal = axisNormal(axis, if (towardPositive) 1.0 else -1.0)
                return CollisionContact(
                    normal = normal,
                    penetration = if (towardPositive) {
                        maximum + radius + SEPARATION_EPSILON - coordinate
                    } else {
                        coordinate - (minimum - radius - SEPARATION_EPSILON)
                    },
                    resolve = { point ->
                        when (axis) {
                            0 -> point.setX(
                                if (towardPositive) maximum + radius + SEPARATION_EPSILON
                                else minimum - radius - SEPARATION_EPSILON,
                            )

                            1 -> point.setY(
                                if (towardPositive) maximum + radius + SEPARATION_EPSILON
                                else minimum - radius - SEPARATION_EPSILON,
                            )

                            else -> point.setZ(
                                if (towardPositive) maximum + radius + SEPARATION_EPSILON
                                else minimum - radius - SEPARATION_EPSILON,
                            )
                        }
                    },
                )
            }
        }
    }
}
