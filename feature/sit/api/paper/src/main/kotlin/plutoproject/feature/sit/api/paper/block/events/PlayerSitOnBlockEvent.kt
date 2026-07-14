package plutoproject.feature.sit.api.paper.block.events

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.sit.api.paper.block.BlockSitAttemptResult
import plutoproject.feature.sit.api.paper.SitOptions
import plutoproject.feature.sit.api.paper.block.BlockSitStrategy
import plutoproject.feature.sit.api.paper.block.SitOnBlockCause

class PlayerSitOnBlockEvent(
    player: Player,
    val options: SitOptions,
    val cause: SitOnBlockCause,
    val attemptResult: BlockSitAttemptResult,
    val seat: Block,
    val strategy: BlockSitStrategy?,
) : PlayerEvent(player), Cancellable {
    @Suppress("UNUSED")
    private companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }

    private var isCancelled = false

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}
