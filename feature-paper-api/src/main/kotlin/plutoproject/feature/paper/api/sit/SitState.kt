package plutoproject.feature.paper.api.sit

enum class SitState {
    NOT_SITTING, ON_BLOCK, ON_PLAYER;

    val isSitting: Boolean
        get() = this == ON_BLOCK || this == ON_PLAYER

    val isSittingOnBlock
        get() = this == ON_BLOCK

    val isSittingOnPlayer
        get() = this == ON_PLAYER
}
