package plutoproject.feature.common.api.whitelist_v2.hook

import java.util.*

/**
 * 白名单系统钩子参数。
 */
sealed class WhitelistHookParam {
    data class GrantWhitelist(
        val uniqueId: UUID,
        val username: String
    ) : WhitelistHookParam()

    data class RevokeWhitelist(val uniqueId: UUID) : WhitelistHookParam()
}
