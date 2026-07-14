package plutoproject.feature.teleport.paper.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.capability.interactive.api.layout.list.ListMenuModel
import plutoproject.feature.teleport.paper.plugin
import plutoproject.feature.teleport.paper.server
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class TeleportRequestScreenModel(private val player: Player) : ListMenuModel<Player>(), Listener {
    var isRequestSent by mutableStateOf(false)
    var requestSentTo by mutableStateOf<Player?>(null)
    var onlinePlayers = mutableStateListOf(*server.onlinePlayers.filter { it != player }.toTypedArray())

    init {
        server.pluginManager.registerSuspendingEvents(this, plugin)
    }

    override fun onDispose() {
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun PlayerJoinEvent.e() {
        onlinePlayers.add(player)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        onlinePlayers.remove(player)
    }

    override suspend fun fetchPageContents(): List<Player> {
        pageCount = ceil(onlinePlayers.size.toDouble() / PAGE_SIZE).toInt()
        return onlinePlayers.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    }
}
