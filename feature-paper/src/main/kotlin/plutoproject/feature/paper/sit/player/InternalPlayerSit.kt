package plutoproject.feature.paper.sit.player

import org.bukkit.NamespacedKey
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.feature.paper.api.sit.player.PlayerStack
import plutoproject.feature.paper.sit.player.contexts.PlayerSitContext

interface InternalPlayerSit : PlayerSit {
    val seatEntityMarkerKey: NamespacedKey

    fun getContext(player: Player): PlayerSitContext?

    fun removeContext(player: Player)

    fun setContext(player: Player, newContext: PlayerSitContext)

    fun removeStack(stack: PlayerStack)

    fun isSeatEntityInUse(entity: AreaEffectCloud): Boolean

    fun getSeatEntityOwner(entity: AreaEffectCloud): Player?
}
