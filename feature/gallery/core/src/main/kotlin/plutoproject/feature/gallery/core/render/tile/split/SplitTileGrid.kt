package plutoproject.feature.gallery.core.render.tile.split

import plutoproject.feature.gallery.core.render.MapColorPixelBuffer
import plutoproject.feature.gallery.core.render.tile.TilePixelsView

class SplitTileGrid internal constructor(
    private val source: MapColorPixelBuffer,
    val mapXBlocks: Int,
    val mapYBlocks: Int,
) {
    val tileCount: Int = mapXBlocks * mapYBlocks

    fun getTile(index: Int): TilePixelsView {
        require(index in 0 until tileCount) { "tile index out of range: $index" }

        val tileX = index % mapXBlocks
        val tileY = index / mapXBlocks
        return TilePixelsView(
            source = source.pixels,
            sourceWidth = source.width,
            tileX = tileX,
            tileY = tileY,
        )
    }
}
