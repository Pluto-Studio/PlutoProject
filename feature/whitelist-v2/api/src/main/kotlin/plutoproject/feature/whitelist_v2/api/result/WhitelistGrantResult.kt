package plutoproject.feature.whitelist_v2.api.result

sealed class WhitelistGrantResult {
    object Ok : WhitelistGrantResult()
    object AlreadyGranted : WhitelistGrantResult()

    val isSuccessful: Boolean = this == Ok
}
