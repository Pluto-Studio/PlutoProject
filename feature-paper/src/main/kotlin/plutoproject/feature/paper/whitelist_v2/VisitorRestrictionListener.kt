package plutoproject.feature.paper.whitelist_v2

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.feature.common.api.whitelist_v2.Whitelist

@Suppress("UNUSED")
object VisitorRestrictionListener : Listener {
    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        // 禁止访客发送聊天消息
        if (Whitelist.isKnownVisitor(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerStartSpectatingEntity(event: PlayerStartSpectatingEntityEvent) {
        val player = event.player
        // 禁止访客附身实体
        if (Whitelist.isKnownVisitor(player.uniqueId)) {
            event.isCancelled = true
        }
    }
}
