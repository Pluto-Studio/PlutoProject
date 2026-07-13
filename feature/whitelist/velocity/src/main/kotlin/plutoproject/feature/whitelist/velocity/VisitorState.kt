package plutoproject.feature.whitelist.velocity

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
