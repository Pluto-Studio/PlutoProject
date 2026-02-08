package plutoproject.feature.whitelist_v2.adapter.velocity

import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookParam
import plutoproject.framework.velocity.util.server
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

private fun kickWhenModified(uniqueId: UUID) {
    val player = server.getPlayer(uniqueId).getOrNull() ?: return
    player.disconnect(PLAYER_WHITELIST_STATE_MODIFIED)
}

fun onWhitelistGrant(param: WhitelistHookParam.GrantWhitelist) {
    kickWhenModified(param.uniqueId)
}

fun onWhitelistRevoke(param: WhitelistHookParam.RevokeWhitelist) {
    kickWhenModified(param.uniqueId)
}
