package ink.pmc.essentials.teleport.random

import ink.pmc.essentials.api.teleport.TaskState
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.api.teleport.random.CacheTask
import ink.pmc.essentials.api.teleport.random.RandomTeleportCache
import ink.pmc.essentials.api.teleport.random.RandomTeleportManager
import ink.pmc.essentials.api.teleport.random.RandomTeleportOptions
import kotlinx.coroutines.*
import org.bukkit.Chunk
import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CacheTaskImpl(
    override val world: World,
    override val options: RandomTeleportOptions,
) : CacheTask, KoinComponent {
    private var scope: CoroutineScope? = null
    private var manager = get<RandomTeleportManager>() as RandomTeleportManagerImpl
    private val teleport by inject<TeleportManager>()

    override val id: UUID = UUID.randomUUID()
    override var attempts: Int = 0
    override val cached: MutableSet<Chunk> = ConcurrentHashMap.newKeySet()
    override var state: TaskState = TaskState.PENDING
    override val isPending: Boolean
        get() = state == TaskState.PENDING
    override val isTicking: Boolean
        get() = state == TaskState.TICKING
    override val isFinished: Boolean
        get() = state == TaskState.FINISHED

    override suspend fun tick(): RandomTeleportCache? {
        if (isTicking || isFinished) {
            return null
        }

        return supervisorScope {
            state = TaskState.TICKING
            scope = this
            val random = manager.random(world, options)
            val location = random.location ?: return@supervisorScope null
            val chunks =
                teleport.getRequiredChunks(location, manager.getRandomTeleportOptions(world).chunkPreserveRadius)

            chunks.forEach {
                launch {
                    val chunk = it.getChunkSuspend(location.world)
                    chunk.addTeleportTicket()
                    cached.add(chunk)
                    yield()
                }
            }

            state = TaskState.FINISHED
            RandomTeleportCache(
                id = id,
                world = location.world,
                center = location.chunk,
                preservedChunks = cached,
                attempts = random.attempts,
                location = location,
                options = options
            )
        }
    }

    override fun cancel() {
        scope?.cancel()
        state = TaskState.FINISHED
        cached.forEach { it.removeTeleportTicket() }
    }
}