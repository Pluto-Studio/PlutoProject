package plutoproject.feature.sit.api.paper.block

enum class BlockSitAttemptResult {
    SUCCEED,
    ALREADY_SITTING,
    SEAT_OCCUPIED,
    INVALID_SEAT,
    BLOCKED_BY_BLOCKS;

    val isSucceed: Boolean
        get() = this == SUCCEED
}
