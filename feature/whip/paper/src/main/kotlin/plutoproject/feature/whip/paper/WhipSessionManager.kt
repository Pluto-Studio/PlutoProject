package plutoproject.feature.whip.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import plutoproject.foundation.paper.coroutine.coroutineDispatcher
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

internal class WhipSessionManager(
    private val scope: CoroutineScope,
    private val config: WhipConfig,
) {
    private val sessions = ConcurrentHashMap<UUID, WhipSession>()
    private val trackedSessions = ConcurrentHashMap.newKeySet<WhipSession>()
    private val lock = Any()
    private val accepting = AtomicBoolean(true)
    private val ownershipRadius = ceil(
        (
            config.lengths.maxOrNull()!! +
                config.simulation.sweepThickness +
                HAND_ANCHOR_MARGIN
            ) / CHUNK_SIZE,
    ).toInt().coerceAtLeast(1)

    fun start(player: Player, hand: EquipmentSlot, item: ItemStack) {
        val identity = inspectWhip(item)
        if (identity !is WhipIdentity.Valid || !hand.isHand || !accepting.get()) {
            return
        }

        val session = WhipSession(
            scope = scope,
            player = player,
            hand = hand,
            item = item,
            level = identity.level,
            ownershipRadius = ownershipRadius,
            config = config,
            onFinished = ::onFinished,
        )
        val previous = synchronized(lock) {
            if (!accepting.get()) {
                return
            }
            val current = sessions[player.uniqueId]
            if (current?.matches(hand, item) == true) {
                return
            }
            trackedSessions += session
            sessions.put(player.uniqueId, session)
        }
        previous?.requestStop()
        session.start()
    }

    fun stop(player: Player) {
        remove(player.uniqueId)?.requestStop()
    }

    fun stopIfHand(player: Player, hand: EquipmentSlot) {
        val session = synchronized(lock) {
            sessions[player.uniqueId]?.takeIf { it.hand == hand }?.also {
                sessions.remove(player.uniqueId, it)
            }
        }
        session?.requestStop()
    }

    fun stopIfDropped(player: Player, droppedItem: ItemStack) {
        val session = synchronized(lock) {
            sessions[player.uniqueId]?.takeIf {
                it.hand == EquipmentSlot.HAND && it.matches(droppedItem)
            }?.also {
                sessions.remove(player.uniqueId, it)
            }
        }
        session?.requestStop()
    }

    suspend fun stopAll() {
        accepting.set(false)
        val tracked = synchronized(lock) {
            sessions.clear()
            trackedSessions.toList()
        }
        tracked.forEach(WhipSession::requestStop)
        tracked.map { scope.launch { it.awaitTermination() } }.joinAll()
        tracked.forEach(trackedSessions::remove)
    }

    private fun remove(id: UUID): WhipSession? = synchronized(lock) {
        sessions.remove(id)
    }

    private fun onFinished(session: WhipSession) {
        synchronized(lock) {
            sessions.remove(session.player.uniqueId, session)
            trackedSessions.remove(session)
        }
    }

    private companion object {
        const val CHUNK_SIZE = 16.0
        const val HAND_ANCHOR_MARGIN = 1.0
    }
}

private class WhipSession(
    private val scope: CoroutineScope,
    val player: Player,
    val hand: EquipmentSlot,
    item: ItemStack,
    private val level: WhipLevel,
    private val ownershipRadius: Int,
    config: WhipConfig,
    private val onFinished: (WhipSession) -> Unit,
) {
    private val initialItem = item.clone()
    private val stopRequested = AtomicBoolean(false)
    private val lifecycleLock = Any()
    private val cleanupJobs = CopyOnWriteArrayList<Job>()
    private val renderer = WhipRenderer(scope, cleanupJobs::add)
    private val chain = WhipChain(config.length(level))
    private val simulation = config.simulation
    @Volatile
    private var job: Job? = null

    fun start() {
        synchronized(lifecycleLock) {
            if (stopRequested.get()) {
                return
            }
            val sessionJob = scope.launch(player.coroutineDispatcher) {
                runLoop()
            }
            job = sessionJob
            sessionJob.invokeOnCompletion {
                renderer.cleanup()
                onFinished(this)
            }
        }
    }

    fun matches(otherHand: EquipmentSlot, item: ItemStack): Boolean =
        hand == otherHand && initialItem.isSimilar(item)

    fun matches(item: ItemStack): Boolean = initialItem.isSimilar(item)

    fun requestStop() {
        val (sessionJob, finishImmediately) = synchronized(lifecycleLock) {
            stopRequested.set(true)
            val sessionJob = job
            sessionJob to (sessionJob == null)
        }
        sessionJob?.cancel()
        renderer.cleanup()
        if (finishImmediately) {
            onFinished(this)
        }
    }

    suspend fun awaitTermination() {
        val sessionJob = synchronized(lifecycleLock) { job }
        sessionJob?.join()
        renderer.cleanup()
        awaitCleanupJobs()
    }

    private suspend fun runLoop() {
        try {
            while (!stopRequested.get()) {
                if (!isStillUsing()) {
                    break
                }

                // This is deliberately the only area-ownership gate for a tick. It happens
                // before reading the world or touching blocks, entities, or displays.
                val playerLocation = player.location
                if (!Bukkit.isOwnedByCurrentRegion(playerLocation, ownershipRadius)) {
                    chain.resetMotionHistory()
                    delay(TICK_DELAY_MILLIS)
                    continue
                }

                val world = player.world
                val anchor = handAnchor(player, hand)
                val direction = player.eyeLocation.direction
                val frame = chain.step(anchor, direction, world, simulation)
                renderer.render(world, frame.current)
                delay(TICK_DELAY_MILLIS)
            }
        } finally {
            withContext(NonCancellable) {
                renderer.cleanup()
                awaitCleanupJobs()
                onFinished(this@WhipSession)
            }
        }
    }

    private suspend fun awaitCleanupJobs() {
        // EntityDispatcher intentionally has no retired callback; bound the wait so a
        // retired display owner cannot keep a session or module shutdown alive forever.
        val pending = cleanupJobs.toList()
        if (pending.isEmpty()) {
            return
        }
        withTimeoutOrNull(DISPLAY_CLEANUP_TIMEOUT_MILLIS) {
            pending.joinAll()
        }
        pending.filter(Job::isActive).forEach(Job::cancel)
    }

    private fun isStillUsing(): Boolean {
        if (!player.isOnline || !player.isValid || player.isDead || !player.hasActiveItem()) {
            return false
        }
        if (player.activeItemHand != hand) {
            return false
        }
        val activeItem = player.activeItem
        val heldItem = player.inventory.getItem(hand)
        return inspectWhip(activeItem) is WhipIdentity.Valid &&
            inspectWhip(heldItem) is WhipIdentity.Valid &&
            initialItem.isSimilar(activeItem) &&
            initialItem.isSimilar(heldItem)
    }

    private fun handAnchor(player: Player, hand: EquipmentSlot): Vector {
        val eye = player.eyeLocation
        val view = eye.direction.normalize()
        val right = view.clone().crossProduct(Vector(0.0, 1.0, 0.0))
        if (right.lengthSquared() < 1.0E-8) {
            right.copy(Vector(1.0, 0.0, 0.0))
        } else {
            right.normalize()
        }
        val mainHandIsRight = player.mainHand == org.bukkit.inventory.MainHand.RIGHT
        val rightSide = if (hand == EquipmentSlot.HAND) mainHandIsRight else !mainHandIsRight
        return eye.toVector()
            .add(view.multiply(HAND_FORWARD_OFFSET))
            .add(right.multiply(if (rightSide) HAND_SIDE_OFFSET else -HAND_SIDE_OFFSET))
            .add(Vector(0.0, -HAND_VERTICAL_OFFSET, 0.0))
    }

    private companion object {
        const val TICK_DELAY_MILLIS = 50L
        const val HAND_FORWARD_OFFSET = 0.25
        const val HAND_SIDE_OFFSET = 0.32
        const val HAND_VERTICAL_OFFSET = 0.35
        const val DISPLAY_CLEANUP_TIMEOUT_MILLIS = 1_000L
    }
}
