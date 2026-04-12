package plutoproject.feature.gallery.core.render.tile.split

import plutoproject.feature.gallery.core.render.MapColorPixelBuffer
import plutoproject.feature.gallery.core.render.tile.TILE_SIDE_PIXELS

object DefaultTileSplitter : TileSplitter {
    override fun split(source: MapColorPixelBuffer, widthBlocks: Int, heightBlocks: Int): SplitTileGrid {
        require(source.width == widthBlocks * TILE_SIDE_PIXELS) {
            "width mismatch: width=${source.width}, widthBlocks=$widthBlocks"
        }
        require(source.height == heightBlocks * TILE_SIDE_PIXELS) {
            "height mismatch: height=${source.height}, heightBlocks=$heightBlocks"
        }

        return SplitTileGrid(
            source = source,
            mapXBlocks = widthBlocks,
            mapYBlocks = heightBlocks,
        )
    }
}
