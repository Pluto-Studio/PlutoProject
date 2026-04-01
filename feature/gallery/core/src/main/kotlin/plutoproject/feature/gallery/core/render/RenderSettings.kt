package plutoproject.feature.gallery.core.render

import kotlin.time.Duration
import plutoproject.feature.gallery.core.render.alphacomposite.AlphaCompositor
import plutoproject.feature.gallery.core.render.alphacomposite.DefaultAlphaCompositor
import plutoproject.feature.gallery.core.render.dither.Ditherer
import plutoproject.feature.gallery.core.render.framesample.FixedIntervalFrameSampler
import plutoproject.feature.gallery.core.render.framesample.FrameSampler
import plutoproject.feature.gallery.core.render.quantize.Quantizer
import plutoproject.feature.gallery.core.render.reposition.Repositioner
import plutoproject.feature.gallery.core.render.scale.Scaler
import plutoproject.feature.gallery.core.render.tile.split.DefaultTileSplitter
import plutoproject.feature.gallery.core.render.tile.split.TileSplitter

class RenderComponents(
    val repositioner: Repositioner,
    val scaler: Scaler,
    val alphaCompositor: AlphaCompositor = DefaultAlphaCompositor,
    val quantizer: Quantizer,
    val ditherer: Ditherer,
    val tileSplitter: TileSplitter = DefaultTileSplitter,
)

class BasicRenderSettings(
    val renderComponents: RenderComponents,
    val widthBlocks: Int,
    val heightBlocks: Int,
    val backgroundColor: Int,
) {
    init {
        require(widthBlocks > 0) { "widthBlocks must be > 0" }
        require(heightBlocks > 0) { "heightBlocks must be > 0" }
    }
}

class AnimatedImageRenderSettings(
    val basicSettings: BasicRenderSettings,
    val frameSampler: FrameSampler = FixedIntervalFrameSampler,
    val minFrameDuration: Duration,
    val outputFrameInterval: Duration,
)
