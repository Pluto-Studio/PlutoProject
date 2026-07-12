package plutoproject.capability.interactive.api.modifiers

import plutoproject.capability.interactive.api.layout.Constraints
import plutoproject.capability.interactive.api.util.IntOffset
import plutoproject.capability.interactive.api.util.IntSize

interface LayoutChangingModifier {
    fun modifyPosition(offset: IntOffset): IntOffset = offset

    /** Modify constraints as they appear to parent nodes laying out this builder. */
    fun modifyLayoutConstraints(measuredSize: IntSize, constraints: Constraints): Constraints =
        modifyInnerConstraints(constraints)

    /** Modify constraints as they appear to this builder and its children for layout. */
    fun modifyInnerConstraints(constraints: Constraints): Constraints = constraints
}
