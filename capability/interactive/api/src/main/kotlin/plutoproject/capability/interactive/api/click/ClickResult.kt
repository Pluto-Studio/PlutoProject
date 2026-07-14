package plutoproject.capability.interactive.api.click

data class ClickResult(val cancelBukkitEvent: Boolean? = null) {
    fun mergeWith(other: ClickResult) = ClickResult(
        // Prioritize true > false > null
        cancelBukkitEvent = (cancelBukkitEvent ?: other.cancelBukkitEvent)?.or(other.cancelBukkitEvent ?: false)
    )
}
