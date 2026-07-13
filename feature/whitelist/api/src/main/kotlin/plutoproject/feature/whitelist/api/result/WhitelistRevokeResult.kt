package plutoproject.feature.whitelist.api.result

sealed class WhitelistRevokeResult {
    object Ok : WhitelistRevokeResult()
    object NotGranted : WhitelistRevokeResult()

    val isSuccessful: Boolean = this == Ok
}
