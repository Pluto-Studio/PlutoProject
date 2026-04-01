package plutoproject.feature.gallery.core.render.reposition

import kotlin.math.min

object ContainRepositioner : Repositioner {
    override fun reposition(
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int
    ): RepositionTransform {
        validateDimensions(sourceWidth, sourceHeight, destinationWidth, destinationHeight)

        val scale = min(
            destinationWidth.toDouble() / sourceWidth.toDouble(),
            destinationHeight.toDouble() / sourceHeight.toDouble(),
        )

        val sourceSpanWidth = destinationWidth / scale
        val sourceSpanHeight = destinationHeight / scale
        val sourceStartX = (sourceWidth - sourceSpanWidth) / 2.0
        val sourceStartY = (sourceHeight - sourceSpanHeight) / 2.0

        return RepositionTransform(
            destinationWidth = destinationWidth,
            destinationHeight = destinationHeight,
            sourceStartX = sourceStartX,
            sourceStartY = sourceStartY,
            sourceSpanWidth = sourceSpanWidth,
            sourceSpanHeight = sourceSpanHeight,
        )
    }
}
