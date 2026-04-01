package plutoproject.feature.gallery.core.render

import plutoproject.feature.gallery.core.image.StaticImageData
import plutoproject.feature.gallery.core.render.tile.dedupe.TileDedupeResult
import plutoproject.feature.gallery.core.render.tile.dedupe.TileDeduper
import plutoproject.feature.gallery.core.util.checkpoint

@OptIn(ExperimentalUnsignedTypes::class)
object StaticImageRenderer {
    suspend fun render(pixelBuffer: PixelBuffer, settings: BasicRenderSettings): RenderResult<StaticImageData> {
        if (settings.widthBlocks.toLong() * settings.heightBlocks.toLong() > Int.MAX_VALUE) {
            return RenderResult.TileIndexCountOverflow
        }

        val tileDeduper = TileDeduper()
        val tileGrid = PixelBufferRenderer.render(pixelBuffer, settings)
        val tileIndexes = UShortArray(tileGrid.tileCount)
        checkpoint()

        for (index in 0..<tileGrid.tileCount) {
            checkpoint()
            when (val result = tileDeduper.dedupe(tileGrid.getTile(index))) {
                is TileDedupeResult.Success -> tileIndexes[index] = result.index.toUShort()
                is TileDedupeResult.TilePoolOverflow -> return RenderResult.TilePoolOverflow
            }
        }

        return RenderResult.Success(StaticImageData(tileDeduper.buildTilePool(), tileIndexes))
    }
}
