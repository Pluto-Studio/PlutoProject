package plutoproject.capability.interactive.api.placement.offset

import androidx.compose.runtime.Stable
import plutoproject.capability.interactive.api.modifiers.LayoutChangingModifier
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.util.IntOffset

data class OffsetModifier(
    val offset: IntOffset
) : Modifier.Element<OffsetModifier>, LayoutChangingModifier {
    override fun mergeWith(other: OffsetModifier) = other

    override fun modifyPosition(offset: IntOffset): IntOffset = offset + this.offset
}

@Stable
fun Modifier.offset(x: Int, y: Int) = then(OffsetModifier(IntOffset(x, y)))
