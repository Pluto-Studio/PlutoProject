package plutoproject.feature.paper.sit.player

import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.player.*
import plutoproject.feature.paper.api.sit.player.events.PlayerStackDestroyEvent
import plutoproject.framework.common.util.data.collection.toImmutable

class PlayerStackImpl(
    carrier: Player,
    initialPassenger: Player,
    override val options: StackOptions
) : PlayerStack, KoinComponent {
    private val internalSit by inject<InternalPlayerSit>()
    private val internalPlayers = mutableListOf(carrier, initialPassenger)

    override val carrier: Player
        get() = internalPlayers.first()
    override val players: Collection<Player> = internalPlayers.toImmutable()
    override var isValid: Boolean = true

    override fun getPlayer(index: Int): Player? {
        return internalPlayers.getOrNull(index)
    }

    override fun getPlayerOnTop(): Player {
        return internalPlayers.last()
    }

    override fun getPlayerAtBottom(): Player {
        return internalPlayers.first()
    }

    override fun contains(player: Player): Boolean {
        return internalPlayers.contains(player)
    }

    override fun addPlayer(
        index: Int,
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause
    ): PlayerStackJoinFinalResult {
        TODO("Not yet implemented")
    }

    override fun addPlayerOnTop(
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause
    ): PlayerStackJoinFinalResult {
        return addPlayer(internalPlayers.lastIndex + 1, player, options, cause)
    }

    override fun addPlayerAtBottom(
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause
    ): PlayerStackJoinFinalResult {
        return addPlayer(0, player, options, cause)
    }

    override fun removePlayer(index: Int, cause: PlayerStackQuitCause): Boolean {
        TODO("Not yet implemented")
    }

    override fun removePlayerOnTop(cause: PlayerStackQuitCause): Boolean {
        return removePlayer(internalPlayers.lastIndex, cause)
    }

    override fun removePlayerAtBottom(cause: PlayerStackQuitCause): Boolean {
        return removePlayer(0, cause)
    }

    private fun callDestroyEvent(cause: PlayerStackDestroyCause): PlayerStackDestroyEvent {
        return PlayerStackDestroyEvent(this, cause).apply { callEvent() }
    }

    override fun destroy(cause: PlayerStackDestroyCause): Boolean {
        if (callDestroyEvent(cause).isCancelled && cause.isCancellable) {
            return false
        }

        isValid = false

        while (internalPlayers.size > 1) {
            removePlayerOnTop(cause = PlayerStackQuitCause.STACK_DESTROY)
        }

        internalSit.removeContext(carrier)
        internalSit.removeStack(this)
        internalPlayers.clear()

        return true
    }
}
