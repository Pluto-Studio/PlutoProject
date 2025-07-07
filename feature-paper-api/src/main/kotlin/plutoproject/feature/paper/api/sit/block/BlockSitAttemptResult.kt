package plutoproject.feature.paper.api.sit.block

enum class BlockSitAttemptResult {
    SUCCEED,
    ALREADY_SITTING,
    SEAT_OCCUPIED,
    INVALID_SEAT,
    BLOCKED_BY_BLOCKS;

    val isSucceed: Boolean
        get() = this == SUCCEED
}
