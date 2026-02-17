package plutoproject.feature.whitelist_v2.api.result

sealed class WhitelistRevokeResult {
    object Ok : WhitelistRevokeResult()
    object NotGranted : WhitelistRevokeResult()

    val isSuccessful: Boolean = this == Ok
}
