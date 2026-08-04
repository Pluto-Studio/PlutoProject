package plutoproject.feature.whip.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.util.BoundingBox
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.Quaternionf
import org.joml.Vector3f
import plutoproject.foundation.paper.coroutine.coroutineDispatcher
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor
import kotlin.math.min

/** Visual-only BlockDisplay view of a simulated whip chain. */
internal class WhipRenderer(
    private val scope: CoroutineScope,
    private val registerCleanupJob: (Job) -> Unit,
) {
    private val displays = CopyOnWriteArrayList<BlockDisplay>()
    private val visualPoints = ArrayList<Vector>()
    private val displayLock = Any()
    private val cleanupStarted = AtomicBoolean(false)

    fun render(world: World, points: List<Vector>) {
        if (points.size < 2 || cleanupStarted.get()) {
            return
        }

        synchronized(displayLock) {
            if (cleanupStarted.get()) {
                return
            }
            deriveVisualTrajectory(world, points)
            ensureDisplays(world, visualPoints)
            for (index in 0 until visualPoints.lastIndex) {
                val display = displays.getOrNull(index) ?: continue
                updateDisplay(
                    display = display,
                    world = world,
                    start = visualPoints[index],
                    end = visualPoints[index + 1],
                    index = index,
                    segmentCount = visualPoints.lastIndex,
                )
            }
        }
    }

    /** Schedules removal on every display's owning dispatcher and can be called repeatedly. */
    fun cleanup() {
        if (!cleanupStarted.compareAndSet(false, true)) {
            return
        }

        val toRemove = synchronized(displayLock) {
            val snapshot = displays.toList()
            displays.clear()
            snapshot
        }
        toRemove.forEach { display ->
            val cleanupJob = scope.launch(display.coroutineDispatcher) {
                synchronized(displayLock) {
                    if (display.isValid) {
                        display.remove()
                    }
                }
            }
            registerCleanupJob(cleanupJob)
        }
    }

    private fun deriveVisualTrajectory(world: World, physicalPoints: List<Vector>) {
        if (visualPoints.size != physicalPoints.size) {
            visualPoints.clear()
            repeat(physicalPoints.size) { visualPoints += Vector() }
        }

        visualPoints.first().copy(physicalPoints.first())
        visualPoints.last().copy(physicalPoints.last())
        for (index in 1 until physicalPoints.lastIndex) {
            val physical = physicalPoints[index]
            val neighborAverage = physicalPoints[index - 1].clone()
                .add(physicalPoints[index + 1])
                .multiply(0.5)
            val displacement = neighborAverage.subtract(physical).multiply(SMOOTHING_STRENGTH)
            val maximumDisplacement = min(
                physical.distance(physicalPoints[index - 1]),
                physical.distance(physicalPoints[index + 1]),
            ) * MAX_SMOOTHING_DISPLACEMENT_FRACTION
            if (displacement.lengthSquared() > maximumDisplacement * maximumDisplacement) {
                displacement.normalize().multiply(maximumDisplacement)
            }

            val candidate = physical.clone().add(displacement)
            visualPoints[index].copy(retractFromTerrain(world, physical, candidate))
        }
    }

    private fun retractFromTerrain(world: World, physical: Vector, candidate: Vector): Vector {
        if (!candidate.x.isFinite() || !candidate.y.isFinite() || !candidate.z.isFinite()) {
            return physical
        }
        if (!overlapsSolidTerrain(world, physical, candidate)) {
            return candidate
        }

        val displacement = candidate.clone().subtract(physical)
        for (step in TERRAIN_RETRACTION_STEPS - 1 downTo 1) {
            val retracted = physical.clone().add(
                displacement.clone().multiply(step.toDouble() / TERRAIN_RETRACTION_STEPS),
            )
            if (!overlapsSolidTerrain(world, physical, retracted)) {
                return retracted
            }
        }
        return physical
    }

    private fun overlapsSolidTerrain(world: World, physical: Vector, candidate: Vector): Boolean {
        val bounds = sweptBounds(physical, candidate, VISUAL_COLLISION_RADIUS)
        val minBlockX = floor(bounds.minX).toInt()
        val maxBlockX = floor(bounds.maxX).toInt()
        val minBlockY = floor(bounds.minY).toInt().coerceAtLeast(world.minHeight)
        val maxBlockY = floor(bounds.maxY).toInt().coerceAtMost(world.maxHeight - 1)
        val minBlockZ = floor(bounds.minZ).toInt()
        val maxBlockZ = floor(bounds.maxZ).toInt()
        if (minBlockY > maxBlockY) {
            return false
        }

        val candidateBounds = BoundingBox.of(
            candidate,
            VISUAL_COLLISION_RADIUS,
            VISUAL_COLLISION_RADIUS,
            VISUAL_COLLISION_RADIUS,
        )
        for (blockX in minBlockX..maxBlockX) {
            for (blockY in minBlockY..maxBlockY) {
                for (blockZ in minBlockZ..maxBlockZ) {
                    val block = world.getBlockAt(blockX, blockY, blockZ)
                    if (!block.isCollidable || block.isLiquid) {
                        continue
                    }
                    if (block.collisionShape.boundingBoxes.any { shape ->
                            BoundingBox(
                                shape.minX + blockX,
                                shape.minY + blockY,
                                shape.minZ + blockZ,
                                shape.maxX + blockX,
                                shape.maxY + blockY,
                                shape.maxZ + blockZ,
                            ).overlaps(candidateBounds)
                        }
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun ensureDisplays(world: World, points: List<Vector>) {
        val segmentCount = points.size - 1
        if (displays.size == segmentCount && displays.all(BlockDisplay::isValid)) {
            return
        }

        displays.forEach { display ->
            if (display.isValid) {
                display.remove()
            }
        }
        displays.clear()
        repeat(segmentCount) { index ->
            val midpoint = points[index].clone().add(points[index + 1]).multiply(0.5)
            val display = world.spawn(
                midpoint.toLocation(world),
                BlockDisplay::class.java,
                CreatureSpawnEvent.SpawnReason.CUSTOM,
            )
            configureDisplay(display, index == segmentCount - 1)
            displays += display
        }
    }

    private fun configureDisplay(display: BlockDisplay, tip: Boolean) {
        display.setBlock((if (tip) Material.BROWN_CONCRETE else Material.BROWN_WOOL).createBlockData())
        display.setGravity(false)
        display.setNoPhysics(true)
        display.setInvulnerable(true)
        display.setPersistent(false)
        display.setSilent(true)
        display.setBillboard(Display.Billboard.FIXED)
        display.setInterpolationDelay(0)
        display.setInterpolationDuration(INTERPOLATION_DURATION_TICKS)
        display.setTeleportDuration(INTERPOLATION_DURATION_TICKS)
        display.setDisplayWidth(2.0f)
        display.setDisplayHeight(2.0f)
    }

    private fun updateDisplay(
        display: BlockDisplay,
        world: World,
        start: Vector,
        end: Vector,
        index: Int,
        segmentCount: Int,
    ) {
        val delta = end.clone().subtract(start)
        val length = delta.length()
        if (!length.isFinite() || length <= DIRECTION_EPSILON) {
            return
        }

        val direction = delta.multiply(1.0 / length)
        val startExtension = if (index == 0) 0.0 else SEGMENT_OVERLAP / 2.0
        val endExtension = if (index == segmentCount - 1) 0.0 else SEGMENT_OVERLAP / 2.0
        val segmentStart = start.clone().subtract(direction.clone().multiply(startExtension))
        val segmentEnd = end.clone().add(direction.clone().multiply(endExtension))
        val segmentDelta = segmentEnd.clone().subtract(segmentStart)
        val segmentLength = segmentDelta.length()
        val segmentDirection = segmentDelta.multiply(1.0 / segmentLength)
        val midpoint = segmentStart.add(segmentEnd).multiply(0.5)
        val rotation = Quaternionf().rotationTo(
            0.0f,
            1.0f,
            0.0f,
            segmentDirection.x.toFloat(),
            segmentDirection.y.toFloat(),
            segmentDirection.z.toFloat(),
        )
        val taperProgress = index.toFloat() / (segmentCount - 1).coerceAtLeast(1)
        val thickness = BODY_THICKNESS + (TIP_THICKNESS - BODY_THICKNESS) * taperProgress
        val scale = Vector3f(thickness, segmentLength.toFloat(), thickness)
        val originCorrection = rotation.transform(Vector3f(scale).mul(0.5f)).negate()

        display.teleport(midpoint.toLocation(world))
        display.setTransformation(
            Transformation(
                originCorrection,
                rotation,
                scale,
                Quaternionf(),
            ),
        )
    }

    private fun sweptBounds(start: Vector, end: Vector, radius: Double): BoundingBox = BoundingBox(
        min(start.x, end.x) - radius,
        min(start.y, end.y) - radius,
        min(start.z, end.z) - radius,
        maxOf(start.x, end.x) + radius,
        maxOf(start.y, end.y) + radius,
        maxOf(start.z, end.z) + radius,
    )

    private companion object {
        const val INTERPOLATION_DURATION_TICKS = 2
        const val SMOOTHING_STRENGTH = 0.5
        const val MAX_SMOOTHING_DISPLACEMENT_FRACTION = 0.2
        const val TERRAIN_RETRACTION_STEPS = 4
        const val SEGMENT_OVERLAP = 0.025
        const val BODY_THICKNESS = 0.13f
        const val TIP_THICKNESS = 0.10f
        const val VISUAL_COLLISION_RADIUS = BODY_THICKNESS / 2.0
        const val DIRECTION_EPSILON = 1.0E-5
    }
}
