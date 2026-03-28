package plutoproject.feature.gallery.adapter.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class GalleryConfig(
    val display: DisplaySettings = DisplaySettings(),
    val send: SendSettings = SendSettings(),
)

data class DisplaySettings(
    val maxFramesPerSecond: Int = 20
)

data class SendSettings(
    val maxQueueSize: Int = 256,
    val maxUpdatesInSpan: Int = 10,
    val updateLimitSpan: Duration = 50.milliseconds,
)
