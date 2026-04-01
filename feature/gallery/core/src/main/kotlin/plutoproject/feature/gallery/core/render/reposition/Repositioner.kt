package plutoproject.feature.gallery.core.render.reposition

interface Repositioner {
    fun reposition(
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int
    ): RepositionTransform
}
