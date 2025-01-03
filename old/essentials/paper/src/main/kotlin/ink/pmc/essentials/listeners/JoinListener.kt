package ink.pmc.essentials.listeners

import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.framework.player.db.PlayerDb
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object JoinListener : Listener, KoinComponent {
    private val config by inject<EssentialsConfig>()
    private val joinConfig by lazy { config.join }

    private suspend fun Player.isItemGiven(name: String): Boolean {
        val db = PlayerDb.getOrCreate(uniqueId)
        return db.getBoolean("essentials.${config.serverName}.join.item.$name")
    }

    private suspend fun Player.setItemGiven(name: String) {
        val db = PlayerDb.getOrCreate(uniqueId)
        db["essentials.${config.serverName}.join.item.$name"] = true
        db.update()
    }
}