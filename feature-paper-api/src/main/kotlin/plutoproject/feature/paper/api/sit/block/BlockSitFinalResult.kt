package plutoproject.feature.paper.api.sit.block

enum class BlockSitFinalResult {
    SUCCEED,
    ALREADY_SITTING,
    SEAT_OCCUPIED,
    INVALID_SEAT,
    BLOCKED_BY_BLOCKS,
    CANCELLED_BY_PLUGIN;

    val isSucceed: Boolean
        get() = this == SUCCEED
}
