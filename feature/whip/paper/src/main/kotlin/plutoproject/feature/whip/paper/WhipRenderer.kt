package plutoproject.feature.whip.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.Quaternionf
import org.joml.Vector3f
import plutoproject.foundation.paper.coroutine.coroutineDispatcher
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/** Visual-only BlockDisplay view of a simulated whip chain. */
internal class WhipRenderer(
    private val scope: CoroutineScope,
    private val registerCleanupJob: (Job) -> Unit,
) {
    private val displays = CopyOnWriteArrayList<BlockDisplay>()
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
            ensureDisplays(world, points)
            for (index in 0 until points.lastIndex) {
                val start = points[index]
                val end = points[index + 1]
                val display = displays.getOrNull(index) ?: continue
                updateDisplay(display, world, start, end, index == points.lastIndex - 1)
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
        display.setInterpolationDuration(2)
        display.setTeleportDuration(0)
        display.setDisplayWidth(2.0f)
        display.setDisplayHeight(2.0f)
    }

    private fun updateDisplay(
        display: BlockDisplay,
        world: World,
        start: Vector,
        end: Vector,
        tip: Boolean,
    ) {
        val delta = end.clone().subtract(start)
        val length = delta.length()
        if (!length.isFinite() || length <= DIRECTION_EPSILON) {
            return
        }

        val direction = delta.multiply(1.0 / length)
        val midpoint = start.clone().add(end).multiply(0.5)
        val rotation = Quaternionf().rotationTo(
            0.0f,
            1.0f,
            0.0f,
            direction.x.toFloat(),
            direction.y.toFloat(),
            direction.z.toFloat(),
        )
        val thickness = if (tip) TIP_THICKNESS else BODY_THICKNESS
        display.teleport(midpoint.toLocation(world))
        display.setTransformation(
            Transformation(
                Vector3f(0.0f, 0.0f, 0.0f),
                rotation,
                Vector3f(thickness, length.toFloat(), thickness),
                Quaternionf(),
            ),
        )
    }

    private companion object {
        const val BODY_THICKNESS = 0.13f
        const val TIP_THICKNESS = 0.10f
        const val DIRECTION_EPSILON = 1.0E-5
    }
}
