package plutoproject.framework.paper.api.bridge.player

import org.bukkit.entity.Player
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.player.BridgePlayer

fun Player.toBridge(): BridgePlayer {
    return Bridge.getPlayer(uniqueId) ?: error("Unexpected")
}
