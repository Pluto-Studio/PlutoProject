package plutoproject.feature.whitelist_v2.adapter.velocity

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
