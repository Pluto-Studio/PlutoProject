package plutoproject.feature.gallery.adapter.common

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

data class AnimatedDisplaySettings(
    val maxFramesPerSecond: Int = 20,
)

data class StaticDisplaySettings(
    val updateInterval: Duration = 50.milliseconds,
)

data class DisplaySettings(
    val animated: AnimatedDisplaySettings = AnimatedDisplaySettings(),
    val static: StaticDisplaySettings = StaticDisplaySettings(),
    val visibleDistance: Double = 64.0,
)

data class SendSettings(
    val maxQueueSize: Int = 256,
    val maxUpdatesInSpan: Int = 10,
    val updateLimitSpan: Duration = 50.milliseconds,
)

data class DecodeSettings(
    val maxParallelTasks: Int = 2,
    val maxBytes: Int = 10_485_760, // 10 * 1024 * 1024
    val maxPixels: Int = 16_777_216, // 4096 * 4096
    val maxFrames: Int = 200,
)

enum class RepositionMode {
    COVER, CONTAIN, STRETCH
}

enum class ScaleMode {
    BILINEAR
}

data class RenderSettings(
    val maxParallelTasks: Int = 4,
    val defaultRepositionMode: RepositionMode = RepositionMode.CONTAIN,
    val defaultScaleMode: ScaleMode = ScaleMode.BILINEAR
)
