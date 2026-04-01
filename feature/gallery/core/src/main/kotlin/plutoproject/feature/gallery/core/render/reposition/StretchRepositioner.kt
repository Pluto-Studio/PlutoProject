package plutoproject.feature.gallery.core.render.reposition

object StretchRepositioner : Repositioner {
    override fun reposition(
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int
    ): RepositionTransform {
        validateDimensions(sourceWidth, sourceHeight, destinationWidth, destinationHeight)

        return RepositionTransform(
            destinationWidth = destinationWidth,
            destinationHeight = destinationHeight,
            sourceStartX = 0.0,
            sourceStartY = 0.0,
            sourceSpanWidth = sourceWidth.toDouble(),
            sourceSpanHeight = sourceHeight.toDouble(),
        )
    }
}
