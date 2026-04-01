package plutoproject.feature.gallery.adapter.common

import plutoproject.feature.gallery.core.render.RenderComponents
import plutoproject.feature.gallery.core.render.dither.Ditherer
import plutoproject.feature.gallery.core.render.dither.FloydSteinbergDitherer
import plutoproject.feature.gallery.core.render.dither.OrderedBayerDitherer
import plutoproject.feature.gallery.core.render.quantize.NearestColorQuantizer
import plutoproject.feature.gallery.core.render.quantize.Quantizer
import plutoproject.feature.gallery.core.render.quantize.Rgb565Quantizer
import plutoproject.feature.gallery.core.render.reposition.ContainRepositioner
import plutoproject.feature.gallery.core.render.reposition.CoverRepositioner
import plutoproject.feature.gallery.core.render.reposition.Repositioner
import plutoproject.feature.gallery.core.render.reposition.StretchRepositioner
import plutoproject.feature.gallery.core.render.scale.BilinearScaler
import plutoproject.feature.gallery.core.render.scale.Scaler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class GalleryConfig(
    val mapIdRange: MapIdRangeSettings = MapIdRangeSettings(),
    val decode: DecodeSettings = DecodeSettings(),
    val render: RenderSettings = RenderSettings(),
    val display: DisplaySettings = DisplaySettings(),
    val send: SendSettings = SendSettings(),
)

data class MapIdRangeSettings(
    val start: Int = 100_000_000,
    val end: Int = 100_499_999,
)

data class DecodeSettings(
    val maxParallelTasks: Int = 2,
    val maxBytes: Int = 10_485_760, // 10 * 1024 * 1024
    val maxPixels: Int = 16_777_216, // 4096 * 4096
    val maxFrames: Int = 200,
)

data class RenderSettings(
    val maxParallelTasks: Int = 4,
    val defaultRepositionMode: RepositionMode = RepositionMode.CONTAIN,
    val defaultScaleMode: ScaleMode = ScaleMode.BILINEAR,
    val defaultQuantizeMode: QuantizeMode = QuantizeMode.RGB565,
    val defaultDitherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
) {
    val renderComponents: RenderComponents
        get() = RenderComponents(
            repositioner = defaultRepositionMode.repositioner,
            scaler = defaultScaleMode.scaler,
            quantizer = defaultQuantizeMode.quantizer,
            ditherer = defaultDitherMode.ditherer
        )
}

enum class RepositionMode {
    COVER, CONTAIN, STRETCH;

    val repositioner: Repositioner
        get() = when (this) {
            COVER -> CoverRepositioner
            CONTAIN -> ContainRepositioner
            STRETCH -> StretchRepositioner
        }
}

enum class ScaleMode {
    BILINEAR;

    val scaler: Scaler
        get() = when (this) {
            BILINEAR -> BilinearScaler
        }
}

enum class QuantizeMode {
    RGB565, NEAREST_COLOR;

    val quantizer: Quantizer
        get() = when (this) {
            RGB565 -> Rgb565Quantizer
            NEAREST_COLOR -> NearestColorQuantizer
        }
}

enum class DitherMode {
    FLOYD_STEINBERG, ORDERED_BAYER;

    val ditherer: Ditherer
        get() = when (this) {
            FLOYD_STEINBERG -> FloydSteinbergDitherer
            ORDERED_BAYER -> OrderedBayerDitherer
        }
}

data class DisplaySettings(
    val static: StaticDisplaySettings = StaticDisplaySettings(),
    val animated: AnimatedDisplaySettings = AnimatedDisplaySettings(),
    val visibleDistance: Double = 64.0,
)

data class StaticDisplaySettings(
    val updateInterval: Duration = 50.milliseconds,
)

data class AnimatedDisplaySettings(
    val maxFramesPerSecond: Int = 20,
)

data class SendSettings(
    val maxQueueSize: Int = 256,
    val maxUpdatesInSpan: Int = 10,
    val updateLimitSpan: Duration = 50.milliseconds,
)
