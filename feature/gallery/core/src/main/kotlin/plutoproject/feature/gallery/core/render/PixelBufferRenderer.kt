package plutoproject.feature.gallery.core.render

import plutoproject.feature.gallery.core.render.tile.split.SplitTileGrid
import plutoproject.feature.gallery.core.util.checkpoint

object PixelBufferRenderer {
    suspend fun render(pixelBuffer: PixelBuffer, settings: BasicRenderSettings): SplitTileGrid {
        checkpoint()

        val targetResolution = calcTargetResolution(settings.widthBlocks, settings.heightBlocks)
        val workspace = RenderWorkspace(pixelBuffer.width, pixelBuffer.height, pixelBuffer)
        checkpoint()

        val transform = settings.renderComponents.repositioner.reposition(
            sourceWidth = workspace.width,
            sourceHeight = workspace.height,
            destinationWidth = targetResolution.width,
            destinationHeight = targetResolution.height,
        )
        checkpoint()

        settings.renderComponents.scaler.scale(workspace, transform)
        checkpoint()

        settings.renderComponents.alphaCompositor.composite(workspace, settings.backgroundColor)
        checkpoint()

        settings.renderComponents.ditherer.dither(workspace, settings.renderComponents.quantizer)
        checkpoint()

        return settings.renderComponents.tileSplitter.split(
            checkNotNull(workspace.mapColorPixelBuffer) { "mapColorPixelBuffer must not be null after dithering" },
            settings.widthBlocks,
            settings.heightBlocks
        )
    }
}
