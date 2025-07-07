package plutoproject.feature.paper.api.sit.player

import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.SitOptions

interface PlayerStack {
    val options: StackOptions
    val carrier: Player
    val players: Collection<Player>
    val isValid: Boolean

    fun getPlayer(index: Int): Player?

    fun getPlayerOnTop(): Player

    fun getPlayerAtBottom(): Player

    fun contains(player: Player): Boolean

    fun indexOf(player: Player): Int?

    fun addPlayer(
        index: Int,
        player: Player,
        options: SitOptions = SitOptions(),
        cause: PlayerStackJoinCause = PlayerStackJoinCause.PLUGIN
    ): PlayerStackJoinFinalResult

    fun addPlayerOnTop(
        player: Player,
        options: SitOptions = SitOptions(),
        cause: PlayerStackJoinCause = PlayerStackJoinCause.PLUGIN
    ): PlayerStackJoinFinalResult

    fun addPlayerAtBottom(
        player: Player,
        options: SitOptions = SitOptions(),
        cause: PlayerStackJoinCause = PlayerStackJoinCause.PLUGIN
    ): PlayerStackJoinFinalResult

    fun removePlayer(index: Int, cause: PlayerStackQuitCause = PlayerStackQuitCause.PLUGIN): Boolean

    fun removePlayer(player: Player, cause: PlayerStackQuitCause = PlayerStackQuitCause.PLUGIN): Boolean

    fun removePlayerOnTop(cause: PlayerStackQuitCause = PlayerStackQuitCause.PLUGIN): Boolean

    fun removePlayerAtBottom(cause: PlayerStackQuitCause = PlayerStackQuitCause.PLUGIN): Boolean

    fun destroy(cause: PlayerStackDestroyCause = PlayerStackDestroyCause.PLUGIN): Boolean
}
