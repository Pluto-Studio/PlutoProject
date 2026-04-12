package plutoproject.feature.gallery.core.render.reposition

internal fun validateDimensions(
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
