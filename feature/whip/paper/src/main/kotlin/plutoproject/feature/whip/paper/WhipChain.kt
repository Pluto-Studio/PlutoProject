package plutoproject.feature.whip.paper

import org.bukkit.World
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

internal data class WhipSimulationFrame(
    val previous: List<Vector>,
    val current: List<Vector>,
    val velocities: List<Vector>,
    val hasMotionHistory: Boolean,
)

/**
 * A small position-based rope. The renderer and later combat code consume only its snapshots;
 * neither of them needs to know how the points are integrated.
 */
internal class WhipChain(
    private val length: Double,
) {
    val segmentCount: Int = ceil(length / MAX_REST_SPACING).toInt().coerceAtLeast(1)
    private val spacing: Double = length / segmentCount
    private val discontinuityThreshold: Double = length * DISCONTINUITY_LENGTH_FRACTION
    private val positions = MutableList(segmentCount + 1) { Vector() }
    private val verletPrevious = MutableList(segmentCount + 1) { Vector() }
    private var previousAnchor: Vector? = null
    private var initialized = false

    fun resetMotionHistory() {
        initialized = false
        previousAnchor = null
    }

    fun step(
        anchor: Vector,
        direction: Vector,
        world: World,
        simulation: WhipSimulationConfig,
    ): WhipSimulationFrame {
        val guideDirection = normalizedDirection(direction)
        var hasMotionHistory = initialized
        if (!initialized || isDiscontinuous(anchor)) {
            initialize(anchor, guideDirection)
            hasMotionHistory = false
        }

        val framePrevious = positions.map(Vector::clone)
        positions[0].copy(anchor)
        verletPrevious[0].copy(anchor)

        for (index in 1 until positions.size) {
            val position = positions[index]
            val previous = verletPrevious[index]
            val velocity = position.clone().subtract(previous).multiply(simulation.damping)
            previous.copy(position)
            position.add(velocity)
            position.setY(position.y - simulation.gravity)
        }

        val guideStrengthPerIteration = 1.0 - (1.0 - simulation.guideStrength).pow(
            1.0 / simulation.constraintIterations,
        )
        repeat(simulation.constraintIterations) {
            positions[0].copy(anchor)
            applyRootGuide(anchor, guideDirection, guideStrengthPerIteration)
            enforceLinkConstraints()
            positions[0].copy(anchor)
            for (index in 1 until positions.size) {
                TerrainCollision.resolve(
                    position = positions[index],
                    previous = verletPrevious[index],
                    world = world,
                    radius = simulation.sweepThickness / 2.0,
                )
            }
        }
        positions[0].copy(anchor)
        previousAnchor = anchor.clone()

        val frameCurrent = positions.map(Vector::clone)
        val velocities = frameCurrent.zip(framePrevious) { current, previous ->
            current.clone().subtract(previous)
        }
        return WhipSimulationFrame(
            previous = framePrevious,
            current = frameCurrent,
            velocities = velocities,
            hasMotionHistory = hasMotionHistory,
        )
    }

    private fun isDiscontinuous(anchor: Vector): Boolean {
        val previous = previousAnchor ?: return false
        return previous.distanceSquared(anchor) > discontinuityThreshold * discontinuityThreshold
    }

    private fun initialize(anchor: Vector, guideDirection: Vector) {
        positions[0].copy(anchor)
        verletPrevious[0].copy(anchor)

        var point = anchor.clone()
        for (index in 1 until positions.size) {
            val droopProgress = (index - 1).toDouble() / (segmentCount - 1).coerceAtLeast(1)
            val segmentDirection = guideDirection.clone()
                .multiply(1.0 - INITIAL_TIP_DROOP * droopProgress)
                .add(DOWNWARD_DIRECTION.clone().multiply(INITIAL_TIP_DROOP * droopProgress))
                .normalize()
            point.add(segmentDirection.multiply(spacing))
            positions[index].copy(point)
            verletPrevious[index].copy(point)
        }
        previousAnchor = anchor.clone()
        initialized = true
    }

    private fun applyRootGuide(anchor: Vector, direction: Vector, strength: Double) {
        if (strength <= 0.0) {
            return
        }
        val target = anchor.clone().add(direction.clone().multiply(spacing))
        positions[1].add(target.subtract(positions[1]).multiply(strength))
    }

    private fun normalizedDirection(direction: Vector): Vector {
        val normalized = direction.clone()
        return if (normalized.lengthSquared() < DIRECTION_EPSILON) {
            DEFAULT_DIRECTION.clone()
        } else {
            normalized.normalize()
        }
    }

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

    private companion object {
        const val MAX_REST_SPACING = 0.5
        const val DISCONTINUITY_LENGTH_FRACTION = 0.5
        const val INITIAL_TIP_DROOP = 0.65
        const val DIRECTION_EPSILON = 1.0E-8
        val DEFAULT_DIRECTION = Vector(0.0, 0.0, 1.0)
        val DOWNWARD_DIRECTION = Vector(0.0, -1.0, 0.0)
    }
}

private object TerrainCollision {
    private const val MAX_RESOLUTION_PASSES = 3
    private const val SEPARATION_EPSILON = 1.0E-4

    fun resolve(
        position: Vector,
        previous: Vector,
        world: World,
        radius: Double,
    ) {
        if (!position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()) {
            return
        }

        repeat(MAX_RESOLUTION_PASSES) {
            val pointBounds = BoundingBox.of(position, radius, radius, radius)
            var best: CollisionContact? = null
            val minBlockX = floor(pointBounds.minX).toInt()
            val maxBlockX = floor(pointBounds.maxX).toInt()
            val minBlockY = floor(pointBounds.minY).toInt().coerceAtLeast(world.minHeight)
            val maxBlockY = floor(pointBounds.maxY).toInt().coerceAtMost(world.maxHeight - 1)
            val minBlockZ = floor(pointBounds.minZ).toInt()
            val maxBlockZ = floor(pointBounds.maxZ).toInt()

            if (minBlockY > maxBlockY) {
                return@repeat
            }

            for (blockX in minBlockX..maxBlockX) {
                for (blockY in minBlockY..maxBlockY) {
                    for (blockZ in minBlockZ..maxBlockZ) {
                        val block = world.getBlockAt(blockX, blockY, blockZ)
                        if (!block.isCollidable || block.isLiquid) {
                            continue
                        }
                        for (relativeBox in block.collisionShape.boundingBoxes) {
                            val blockBox = BoundingBox(
                                relativeBox.minX + blockX,
                                relativeBox.minY + blockY,
                                relativeBox.minZ + blockZ,
                                relativeBox.maxX + blockX,
                                relativeBox.maxY + blockY,
                                relativeBox.maxZ + blockZ,
                            )
                            if (!blockBox.overlaps(pointBounds)) {
                                continue
                            }

                            val overlapX = min(pointBounds.maxX, blockBox.maxX) -
                                maxOf(pointBounds.minX, blockBox.minX)
                            val overlapY = min(pointBounds.maxY, blockBox.maxY) -
                                maxOf(pointBounds.minY, blockBox.minY)
                            val overlapZ = min(pointBounds.maxZ, blockBox.maxZ) -
                                maxOf(pointBounds.minZ, blockBox.minZ)
                            if (overlapX <= 0.0 || overlapY <= 0.0 || overlapZ <= 0.0) {
                                continue
                            }

                            val contact = CollisionContact.from(
                                position = position,
                                blockBox = blockBox,
                                overlapX = overlapX,
                                overlapY = overlapY,
                                overlapZ = overlapZ,
                                radius = radius,
                            )
                            val currentBest = best
                            if (currentBest == null || contact.penetration < currentBest.penetration) {
                                best = contact
                            }
                        }
                    }
                }
            }

            val contact = best ?: return@repeat
            val velocity = position.clone().subtract(previous)
            contact.resolve(position)
            val normalVelocity = velocity.dot(contact.normal)
            val slideVelocity = if (normalVelocity < 0.0) {
                velocity.subtract(contact.normal.clone().multiply(normalVelocity))
            } else {
                velocity
            }
            previous.copy(position).subtract(slideVelocity)
        }
    }

    private data class CollisionContact(
        val normal: Vector,
        val penetration: Double,
        val resolve: (Vector) -> Unit,
    ) {
        companion object {
            fun from(
                position: Vector,
                blockBox: BoundingBox,
                overlapX: Double,
                overlapY: Double,
                overlapZ: Double,
                radius: Double,
            ): CollisionContact {
                val penetration = minOf(overlapX, overlapY, overlapZ)
                return when (penetration) {
                    overlapX -> {
                        val towardPositive = position.x >= blockBox.centerX
                        CollisionContact(
                            normal = Vector(if (towardPositive) 1.0 else -1.0, 0.0, 0.0),
                            penetration = penetration,
                            resolve = { point ->
                                point.setX(
                                    if (towardPositive) {
                                        blockBox.maxX + radius + SEPARATION_EPSILON
                                    } else {
                                        blockBox.minX - radius - SEPARATION_EPSILON
                                    },
                                )
                            },
                        )
                    }

                    overlapY -> {
                        val towardPositive = position.y >= blockBox.centerY
                        CollisionContact(
                            normal = Vector(0.0, if (towardPositive) 1.0 else -1.0, 0.0),
                            penetration = penetration,
                            resolve = { point ->
                                point.setY(
                                    if (towardPositive) {
                                        blockBox.maxY + radius + SEPARATION_EPSILON
                                    } else {
                                        blockBox.minY - radius - SEPARATION_EPSILON
                                    },
                                )
                            },
                        )
                    }

                    else -> {
                        val towardPositive = position.z >= blockBox.centerZ
                        CollisionContact(
                            normal = Vector(0.0, 0.0, if (towardPositive) 1.0 else -1.0),
                            penetration = penetration,
                            resolve = { point ->
                                point.setZ(
                                    if (towardPositive) {
                                        blockBox.maxZ + radius + SEPARATION_EPSILON
                                    } else {
                                        blockBox.minZ - radius - SEPARATION_EPSILON
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
