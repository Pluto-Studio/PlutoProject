package plutoproject.feature.gallery.core.render.tile

import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.render.RenderStatus

internal data class TileSplitAndDedupeResult(
    val status: RenderStatus,
    val imageData: StaticImageData?,
)

/**
 * 将整图 mapColor 像素切分为 128x128 tile，并按顺序写入 tileIndexes。
 *
 * tile 顺序约定：从左到右、从上到下。
 */
internal object TileSplitter {
    fun splitAndDedupe(
        mapColorPixels: ByteArray,
        width: Int,
        height: Int,
        mapXBlocks: Int,
        mapYBlocks: Int,
    ): TileSplitAndDedupeResult {
        require(width == mapXBlocks * TILE_SIDE_PIXELS) {
            "width mismatch: width=$width, mapXBlocks=$mapXBlocks"
        }
        require(height == mapYBlocks * TILE_SIDE_PIXELS) {
            "height mismatch: height=$height, mapYBlocks=$mapYBlocks"
        }
        require(mapColorPixels.size == width * height) {
            "mapColorPixels size mismatch: expected=${width * height}, actual=${mapColorPixels.size}"
        }

        val tileCount = mapXBlocks * mapYBlocks
        val tileIndexes = ShortArray(tileCount)
        val tileWorkBuffer = ByteArray(TILE_PIXEL_COUNT)
        val deduper = TileDeduper()

        var tileLinearIndex = 0
        var tileY = 0
        while (tileY < mapYBlocks) {
            var tileX = 0
            while (tileX < mapXBlocks) {
                copyTile(
                    source = mapColorPixels,
                    sourceWidth = width,
                    tileX = tileX,
                    tileY = tileY,
                    destination = tileWorkBuffer,
                )

                when (val dedupe = deduper.dedupe(tileWorkBuffer)) {
                    is TileDedupeResult.Indexed -> {
                        tileIndexes[tileLinearIndex] = dedupe.tilePoolIndex.toShort()
                    }

                    TileDedupeResult.UniqueTileOverflow -> {
                        return TileSplitAndDedupeResult(
                            status = RenderStatus.UNIQUE_TILE_OVERFLOW,
                            imageData = null,
                        )
                    }
                }

                tileLinearIndex++
                tileX++
            }
            tileY++
        }

        return TileSplitAndDedupeResult(
            status = RenderStatus.SUCCEED,
            imageData = StaticImageData(
                tilePool = deduper.buildTilePool(),
                tileIndexes = tileIndexes,
            ),
        )
    }
}

private fun copyTile(
    source: ByteArray,
    sourceWidth: Int,
    tileX: Int,
    tileY: Int,
    destination: ByteArray,
) {
    val sourceStartX = tileX * TILE_SIDE_PIXELS
    val sourceStartY = tileY * TILE_SIDE_PIXELS

    var destinationOffset = 0
    var localY = 0
    while (localY < TILE_SIDE_PIXELS) {
        val sourceOffset = (sourceStartY + localY) * sourceWidth + sourceStartX
        source.copyInto(
            destination = destination,
            destinationOffset = destinationOffset,
            startIndex = sourceOffset,
            endIndex = sourceOffset + TILE_SIDE_PIXELS,
        )
        destinationOffset += TILE_SIDE_PIXELS
        localY++
    }
}
