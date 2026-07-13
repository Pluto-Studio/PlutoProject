package plutoproject.feature.sit.paper.player

import org.bukkit.NamespacedKey
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import plutoproject.feature.sit.api.paper.player.PlayerSit
import plutoproject.feature.sit.api.paper.player.PlayerStack
import plutoproject.feature.sit.paper.player.contexts.PlayerSitContext

interface InternalPlayerSit : PlayerSit {
    val seatEntityMarkerKey: NamespacedKey

    fun getContext(player: Player): PlayerSitContext?

    fun removeContext(player: Player)

    fun setContext(player: Player, newContext: PlayerSitContext)

    fun removeStack(stack: PlayerStack)

    fun isSeatEntityInUse(entity: AreaEffectCloud): Boolean

    fun getSeatEntityOwner(entity: AreaEffectCloud): Player?
}
