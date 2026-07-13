package plutoproject.feature.sit.api.paper.player

enum class PlayerStackJoinFinalResult {
    SUCCESS,
    ALREADY_IN,
    CANCELLED_BY_PLUGIN;

    val isSucceed: Boolean
        get() = this == SUCCESS
}
