package plutoproject.feature.gallery.core.render.reposition

class RepositionTransform(
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
        return sourceStartX + ((destinationX + 0.5) / destinationWidth) * sourceSpanWidth
    }

    fun sourceYAt(destinationY: Int): Double {
        require(destinationY in 0 until destinationHeight) {
            "destinationY out of range: y=$destinationY, height=$destinationHeight"
        }
        return sourceStartY + ((destinationY + 0.5) / destinationHeight) * sourceSpanHeight
    }
}
