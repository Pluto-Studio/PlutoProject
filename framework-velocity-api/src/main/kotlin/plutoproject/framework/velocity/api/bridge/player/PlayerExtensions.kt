package plutoproject.framework.velocity.api.bridge.player

import com.velocitypowered.api.proxy.Player
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.player.BridgePlayer

fun Player.toBridge(): BridgePlayer {
    return Bridge.getPlayer(uniqueId) ?: error("Unexpected")
}
