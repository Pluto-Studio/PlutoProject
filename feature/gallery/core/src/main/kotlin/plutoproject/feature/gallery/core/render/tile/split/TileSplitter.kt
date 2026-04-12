package plutoproject.feature.gallery.core.render.tile.split

import plutoproject.feature.gallery.core.render.MapColorPixelBuffer

interface TileSplitter {
    fun split(source: MapColorPixelBuffer, widthBlocks: Int, heightBlocks: Int): SplitTileGrid
}
