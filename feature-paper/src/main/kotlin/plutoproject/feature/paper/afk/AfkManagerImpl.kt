package plutoproject.feature.paper.afk

import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.afk.AfkManager
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.runAsync
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

class AfkManagerImpl : AfkManager, KoinComponent {
    private val conf by inject<AfkConfig>()
    private val manuallyAfkSet = ConcurrentHashMap.newKeySet<Player>()

    override val afkSet: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    override val idleDuration: Duration = conf.idleDuration

    init {
        runAsync {
            while (!disabled) {
                Bukkit.getOnlinePlayers().forEach {
                    val idle = it.idleDuration.toKotlinDuration()
                    if (idle >= idleDuration) set(it, true)
                }
                afkSet.removeIf { !it.isOnline }
                delay(1.seconds)
            }
        }
    }

    private fun isManually(player: Player): Boolean {
        return manuallyAfkSet.contains(player)
    }

    private fun addManually(player: Player) {
        manuallyAfkSet.add(player)
    }

    private fun removeManually(player: Player) {
        manuallyAfkSet.remove(player)
    }

    override fun isAfk(player: Player): Boolean {
        return afkSet.contains(player)
    }

    override fun set(player: Player, state: Boolean, manually: Boolean) {
        if (state && !isAfk(player)) {
            val idle = player.idleDuration.toKotlinDuration()
            if (idle < idleDuration) {
                player.resetIdleDuration()
            }
            afkSet.add(player)
            if (manually) addManually(player)
            Bukkit.broadcast(PLAYER_ENTER_AFK_BROADCAST.replace("<player>", player.name))
            return
        }

        if (!state && isAfk(player)) {
            afkSet.remove(player)
            Bukkit.broadcast(PLAYER_EXIT_AFK_BROADCAST.replace("<player>", player.name))
            removeManually(player)
            return
        }
    }

    override fun toggle(player: Player, manually: Boolean) {
        set(player, !isAfk(player), manually)
    }
}
