package plutoproject.capability.interactive.api.canvas.dialog

import java.util.*

data class DialogElement<T>(
    val id: UUID,
    val element: T,
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is DialogElement<*>) {
            return false
        }
        return other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
