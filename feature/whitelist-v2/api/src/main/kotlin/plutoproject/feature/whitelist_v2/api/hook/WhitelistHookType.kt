package plutoproject.feature.whitelist_v2.api.hook

sealed class WhitelistHookType<T : WhitelistHookParam> {
    object GrantWhitelist : WhitelistHookType<WhitelistHookParam.GrantWhitelist>()
    object RevokeWhitelist : WhitelistHookType<WhitelistHookParam.RevokeWhitelist>()
}
