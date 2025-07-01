package plutoproject.feature.paper.api.sitV2

enum class SitFinalResult {
    SUCCEED,
    FAILED_ALREADY_SITTING,
    FAILED_TARGET_OCCUPIED,
    FAILED_INVALID_TARGET,
    FAILED_BLOCKED_BY_BLOCKS,
    FAILED_CANCELLED_BY_PLUGIN;

    val isSucceed: Boolean
        get() = this == SUCCEED
}
