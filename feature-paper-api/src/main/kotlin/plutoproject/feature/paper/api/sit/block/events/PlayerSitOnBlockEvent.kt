package plutoproject.feature.paper.api.sit.block.events

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import plutoproject.feature.paper.api.sit.block.BlockSitStrategy
import plutoproject.feature.paper.api.sit.SitAttemptResult
import plutoproject.feature.paper.api.sit.SitOptions

class PlayerSitOnBlockEvent(
    player: Player,
    options: SitOptions,
    attemptResult: SitAttemptResult,
    val sittingOn: Block,
    val sitStrategy: BlockSitStrategy?,
) : SitEvent(player, options, attemptResult) {
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
