package plutoproject.feature.whitelist.api.hook

sealed class WhitelistHookType<T : WhitelistHookParam> {
    object GrantWhitelist : WhitelistHookType<WhitelistHookParam.GrantWhitelist>()
    object RevokeWhitelist : WhitelistHookType<WhitelistHookParam.RevokeWhitelist>()
}
