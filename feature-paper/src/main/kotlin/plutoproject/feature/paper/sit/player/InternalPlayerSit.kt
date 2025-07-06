package plutoproject.feature.paper.sit.player

import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.feature.paper.api.sit.player.PlayerStack
import plutoproject.feature.paper.sit.player.contexts.PlayerSitContext

interface InternalPlayerSit : PlayerSit {
    fun getContext(player: Player): PlayerSitContext?

    fun removeContext(player: Player)

    fun setContext(player: Player, newContext: PlayerSitContext)

    fun removeStack(stack: PlayerStack)
}
