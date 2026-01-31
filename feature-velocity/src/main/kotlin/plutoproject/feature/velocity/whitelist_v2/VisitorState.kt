package plutoproject.feature.velocity.whitelist_v2

object VisitorState {
    @Volatile
    var isVisitorModeEnabled: Boolean = false
        private set

    fun toggle(): Boolean {
        isVisitorModeEnabled = !isVisitorModeEnabled
        return isVisitorModeEnabled
    }

    fun setEnabled(enabled: Boolean) {
        isVisitorModeEnabled = enabled
    }
}
