package plutoproject.capability.interactive.api.drag

import plutoproject.capability.interactive.api.modifiers.Modifier

open class DragModifier(
    val merged: Boolean = false,
    val onDrag: (DragScope.() -> Unit),
) : Modifier.Element<DragModifier> {
    override fun mergeWith(other: DragModifier) = DragModifier(merged = true) {
        if (!other.merged) {
            onDrag()
        }
        other.onDrag(this)
    }
}
