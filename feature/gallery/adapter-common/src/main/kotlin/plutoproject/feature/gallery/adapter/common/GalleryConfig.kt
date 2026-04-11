package plutoproject.feature.gallery.adapter.common

import plutoproject.feature.gallery.core.render.RenderComponents
import plutoproject.feature.gallery.core.render.dither.Ditherer
import plutoproject.feature.gallery.core.render.dither.FloydSteinbergDitherer
import plutoproject.feature.gallery.core.render.dither.NoneDitherer
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
import kotlin.time.Duration.Companion.minutes

data class GalleryConfig(
    val image: ImageSettings = ImageSettings(),
    val fileProcessing: FileProcessingSettings = FileProcessingSettings(),
    val upload: UploadSettings = UploadSettings(),
    val decode: DecodeSettings = DecodeSettings(),
    val render: RenderSettings = RenderSettings(),
    val display: DisplaySettings = DisplaySettings(),
    val send: SendSettings = SendSettings(),
)

data class ImageSettings(
    val mapIdRange: MapIdRangeSettings = MapIdRangeSettings(),
    val maxNameLength: Int = 20,
    val maxImagesPerPlayer: Int = 20,
    val maxMapBlocks: Int = 16,
    val staticMaxMapBlocks: Int? = null,
    val animatedMaxMapBlocks: Int? = 1,
    val maxLongEdgeBlocks: Int = 8,
) {
    val effectiveStaticMaxMapBlocks: Int
        get() = staticMaxMapBlocks ?: maxMapBlocks

    val effectiveAnimatedMaxMapBlocks: Int
        get() = animatedMaxMapBlocks ?: maxMapBlocks
}

data class MapIdRangeSettings(
    val start: Int = 100_000_000,
    val end: Int = 100_499_999,
)

data class FileProcessingSettings(
    val maxBytes: Int = 10 * 1024 * 1024, // 10 MiB
    val maxPixels: Int = 16_777_216, // 4096 * 4096
    val maxFrames: Int = 200,
    val allowedFileExtensions: List<String> = listOf("png", "jpg", "jpeg", "webp", "gif"),
    val tempFolderRoot: String = "/tmp/",
)

data class UploadSettings(
    val requestExpireAfter: Duration = 10.minutes,
    val baseUrl: String = "https://gallery.plutoproject.club/",
    val port: Int = 24213,
    val suggestedMaxWidth: Int = 4096,
    val suggestedMaxHeight: Int = 4096,
)

data class DecodeSettings(
    val maxParallelTasks: Int = 2,
)

data class RenderSettings(
    val maxParallelTasks: Int = 4,
    val repositionMode: RepositionMode = RepositionMode.CONTAIN,
    val scaleMode: ScaleMode = ScaleMode.BILINEAR,
    val quantizeMode: QuantizeMode = QuantizeMode.RGB565,
    val ditherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
) {
    val renderComponents: RenderComponents
        get() = RenderComponents(
            repositioner = repositionMode.repositioner,
            scaler = scaleMode.scaler,
            quantizer = quantizeMode.quantizer,
            ditherer = ditherMode.ditherer
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
    NONE, FLOYD_STEINBERG, ORDERED_BAYER;

    val ditherer: Ditherer
        get() = when (this) {
            NONE -> NoneDitherer
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
