package plutoproject.feature.paper.api.sitV2.events

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import plutoproject.feature.paper.api.sitV2.BlockSitStrategy
import plutoproject.feature.paper.api.sitV2.SitOptions
import plutoproject.feature.paper.api.sitV2.SitResult

class PlayerSitOnBlockEvent(
    player: Player,
    options: SitOptions,
    result: SitResult,
    val sittingOn: Block,
    val sitStrategy: BlockSitStrategy,
) : SitEvent(player, options, result) {
    @Suppress("UNUSED")
    private companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }

    private var isCancelled = false

    override fun getHandlers() = Companion.handlers

    override fun isCancelled() = isCancelled

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}
