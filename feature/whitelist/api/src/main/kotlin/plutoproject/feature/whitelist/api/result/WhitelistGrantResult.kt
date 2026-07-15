package plutoproject.feature.whitelist.api.result

sealed class WhitelistGrantResult {
    object Ok : WhitelistGrantResult()
    object AlreadyGranted : WhitelistGrantResult()

    val isSuccessful: Boolean
        get() = this == Ok
}
