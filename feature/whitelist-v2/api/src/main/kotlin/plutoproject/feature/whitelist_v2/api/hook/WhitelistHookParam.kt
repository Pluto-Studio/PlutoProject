package plutoproject.feature.whitelist_v2.api.hook

import java.util.UUID

/**
 * 白名单系统钩子参数。
 */
sealed class WhitelistHookParam {
    data class GrantWhitelist(
        val uniqueId: UUID,
        val username: String,
    ) : WhitelistHookParam()

    data class RevokeWhitelist(
        val uniqueId: UUID,
    ) : WhitelistHookParam()
}
