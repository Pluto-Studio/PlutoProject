package plutoproject.feature.paper.api.sitV2

enum class SitAttemptResult {
    SUCCEED,
    FAILED_ALREADY_SITTING,
    FAILED_TARGET_OCCUPIED,
    FAILED_INVALID_TARGET,
    FAILED_BLOCKED_BY_BLOCKS;

    val isSucceed: Boolean
        get() = this == SUCCEED
}
