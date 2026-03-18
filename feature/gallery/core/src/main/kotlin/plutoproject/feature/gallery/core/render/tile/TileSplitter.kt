package plutoproject.feature.gallery.core.render.tile

import plutoproject.feature.gallery.core.render.RenderStatus
import plutoproject.feature.gallery.core.render.tile.codec.TILE_PIXEL_COUNT
import plutoproject.feature.gallery.core.render.tile.codec.TILE_SIDE_PIXELS
import plutoproject.feature.gallery.core.render.tile.dedupe.TileDedupeResult
import plutoproject.feature.gallery.core.render.tile.dedupe.TileDeduper

internal data class TileSplitAndDedupeResult(
    val status: RenderStatus,
    val tileIndexes: ShortArray?,
)

/**
 * 将整图 mapColor 像素切分为 128x128 tile，并按顺序写入 tileIndexes。
 *
 * tile 顺序约定：从左到右、从上到下。
 */
internal object TileSplitter {
    /**
     * 将整图 mapColor 像素切分为 128x128 tile，并写入传入的 [deduper]。
     *
     * 返回值说明：
     * - [TileSplitAndDedupeResult.tileIndexes] 为当前输入图对应的 tilePool 索引矩阵
     * - 这些索引仅在“同一个 [deduper] 最终 `buildTilePool()` 的结果”中有意义
     *
     * 典型用途：
     * - 静态图：调用方创建一个 deduper，调用本函数一次，再 `buildTilePool()`
     * - 动图：跨帧复用同一个 deduper，调用本函数多次，实现跨帧 dedup
     */
    fun splitAndDedupe(
        mapColorPixels: ByteArray,
        width: Int,
        height: Int,
        mapXBlocks: Int,
        mapYBlocks: Int,
        deduper: TileDeduper,
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
                            tileIndexes = null,
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
            tileIndexes = tileIndexes,
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
