package plutoproject.feature.gallery.core.image

import plutoproject.feature.gallery.core.render.tile.TilePool

@OptIn(ExperimentalUnsignedTypes::class)
class StaticImageData(
    val tilePool: TilePool,
    val tileIndexes: UShortArray
)
