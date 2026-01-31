package plutoproject.feature.common.api.whitelist_v2.hook

sealed class WhitelistHookType<T : WhitelistHookParam> {
    object GrantWhitelist : WhitelistHookType<WhitelistHookParam.GrantWhitelist>()
    object RevokeWhitelist : WhitelistHookType<WhitelistHookParam.RevokeWhitelist>()
}
