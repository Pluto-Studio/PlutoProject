package plutoproject.feature.gallery.core.render.geometry

import kotlin.math.max
import kotlin.math.min
import plutoproject.feature.gallery.core.render.RepositionMode

internal data class DestToSourceTransform(
    val destinationWidth: Int,
    val destinationHeight: Int,
    val sourceStartX: Double,
    val sourceStartY: Double,
    val sourceSpanWidth: Double,
    val sourceSpanHeight: Double,
) {
    init {
        require(destinationWidth > 0) { "destinationWidth must be > 0" }
        require(destinationHeight > 0) { "destinationHeight must be > 0" }
        require(sourceSpanWidth > 0.0) { "sourceSpanWidth must be > 0" }
        require(sourceSpanHeight > 0.0) { "sourceSpanHeight must be > 0" }
    }

    fun sourceXAt(destinationX: Int): Double {
        require(destinationX in 0 until destinationWidth) {
            "destinationX out of range: x=$destinationX, width=$destinationWidth"
        }
        return sourceStartX + ((destinationX + 0.5) / destinationWidth) * sourceSpanWidth - 0.5
    }

    fun sourceYAt(destinationY: Int): Double {
        require(destinationY in 0 until destinationHeight) {
            "destinationY out of range: y=$destinationY, height=$destinationHeight"
        }
        return sourceStartY + ((destinationY + 0.5) / destinationHeight) * sourceSpanHeight - 0.5
    }
}

internal fun interface Repositioner {
    fun reposition(
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int,
    ): DestToSourceTransform
}

internal object CoverRepositioner : Repositioner {
    override fun reposition(
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int,
    ): DestToSourceTransform {
        validateDimensions(sourceWidth, sourceHeight, destinationWidth, destinationHeight)

        val scale = max(
            destinationWidth.toDouble() / sourceWidth.toDouble(),
            destinationHeight.toDouble() / sourceHeight.toDouble(),
        )

        val sourceSpanWidth = destinationWidth / scale
        val sourceSpanHeight = destinationHeight / scale
        val sourceStartX = (sourceWidth - sourceSpanWidth) / 2.0
        val sourceStartY = (sourceHeight - sourceSpanHeight) / 2.0

        return DestToSourceTransform(
            destinationWidth = destinationWidth,
            destinationHeight = destinationHeight,
            sourceStartX = sourceStartX,
            sourceStartY = sourceStartY,
            sourceSpanWidth = sourceSpanWidth,
            sourceSpanHeight = sourceSpanHeight,
        )
    }
}

internal object ContainRepositioner : Repositioner {
    override fun reposition(
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int,
    ): DestToSourceTransform {
        validateDimensions(sourceWidth, sourceHeight, destinationWidth, destinationHeight)

        val scale = min(
            destinationWidth.toDouble() / sourceWidth.toDouble(),
            destinationHeight.toDouble() / sourceHeight.toDouble(),
        )

        val sourceSpanWidth = destinationWidth / scale
        val sourceSpanHeight = destinationHeight / scale
        val sourceStartX = (sourceWidth - sourceSpanWidth) / 2.0
        val sourceStartY = (sourceHeight - sourceSpanHeight) / 2.0

        return DestToSourceTransform(
            destinationWidth = destinationWidth,
            destinationHeight = destinationHeight,
            sourceStartX = sourceStartX,
            sourceStartY = sourceStartY,
            sourceSpanWidth = sourceSpanWidth,
            sourceSpanHeight = sourceSpanHeight,
        )
    }
}

internal object StretchRepositioner : Repositioner {
    override fun reposition(
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int,
    ): DestToSourceTransform {
        validateDimensions(sourceWidth, sourceHeight, destinationWidth, destinationHeight)

        return DestToSourceTransform(
            destinationWidth = destinationWidth,
            destinationHeight = destinationHeight,
            sourceStartX = 0.0,
            sourceStartY = 0.0,
            sourceSpanWidth = sourceWidth.toDouble(),
            sourceSpanHeight = sourceHeight.toDouble(),
        )
    }
}

internal fun repositionerOf(mode: RepositionMode): Repositioner = when (mode) {
    RepositionMode.COVER -> CoverRepositioner
    RepositionMode.CONTAIN -> ContainRepositioner
    RepositionMode.STRETCH -> StretchRepositioner
}

private fun validateDimensions(
    sourceWidth: Int,
    sourceHeight: Int,
    destinationWidth: Int,
    destinationHeight: Int,
) {
    require(sourceWidth > 0) { "sourceWidth must be > 0" }
    require(sourceHeight > 0) { "sourceHeight must be > 0" }
    require(destinationWidth > 0) { "destinationWidth must be > 0" }
    require(destinationHeight > 0) { "destinationHeight must be > 0" }
}
