package plutoproject.feature.teleport.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.craftbukkit.CraftChunk
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.Player
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.worldalias.api.worldalias.WorldAlias
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.koinGet
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.paper.PaperModuleContext
import kotlin.coroutines.CoroutineContext

internal val moduleContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext
internal val moduleScope: CoroutineScope
    get() = currentModuleContext().coroutineScope
internal val plugin
    get() = moduleContext.plugin
internal val server: Server
    get() = plugin.server
internal val teleportManager: TeleportManager
    get() = koinGet()
internal val Server.coroutineContext: CoroutineContext
    get() = plugin.minecraftDispatcher
internal val Player.coroutineContext: CoroutineContext
    get() = plugin.minecraftDispatcher
internal val World.aliasOrName: String
    get() = currentModuleContext().services.getService<WorldAlias>().getAliasOrName(this)

internal fun Player.startScreen(screen: InteractiveScreen) {
    currentModuleContext().services.getService<GuiManager>().startScreen(this, screen)
}

internal suspend fun World.getChunkFromSource(x: Int, z: Int): org.bukkit.Chunk? {
    val result = (this as CraftWorld).handle.chunkSource
        .getChunkFuture(x, z, ChunkStatus.FULL, true)
        .await()
    val chunk = result.orElse(null) as? LevelChunk ?: return null
    return CraftChunk(chunk)
}
