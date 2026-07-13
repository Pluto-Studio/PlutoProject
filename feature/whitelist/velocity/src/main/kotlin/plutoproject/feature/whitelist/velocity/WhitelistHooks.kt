package plutoproject.feature.whitelist.velocity

import com.velocitypowered.api.proxy.ProxyServer
import plutoproject.feature.whitelist.api.hook.WhitelistHookParam
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

private fun ProxyServer.kickWhenModified(uniqueId: UUID) {
    val player = getPlayer(uniqueId).getOrNull() ?: return
    player.disconnect(PLAYER_WHITELIST_STATE_MODIFIED)
}

fun ProxyServer.onWhitelistGrant(param: WhitelistHookParam.GrantWhitelist) {
    kickWhenModified(param.uniqueId)
}

fun ProxyServer.onWhitelistRevoke(param: WhitelistHookParam.RevokeWhitelist) {
    kickWhenModified(param.uniqueId)
}
