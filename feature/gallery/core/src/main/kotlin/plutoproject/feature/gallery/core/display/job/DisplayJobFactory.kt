package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import java.time.Clock
import kotlin.time.Duration

class DisplayJobFactory(
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val displayManager: DisplayManager,
    private val clock: Clock,
    private val animatedMaxFramesPerSecond: Int,
    private val visibleDistance: Double,
    private val staticUpdateInterval: Duration,
) {
    fun create(
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): DisplayJob {
        require(image.id == imageDataEntry.imageId) {
            "Image and ImageDataEntry mismatch: image.id=${image.id}, imageId=${imageDataEntry.imageId}"
        }
        require(image.type == imageDataEntry.type) {
            "Image and ImageDataEntry type mismatch: image.type=${image.type}, entry.type=${imageDataEntry.type}"
        }

        return when (image.type) {
            ImageType.STATIC -> StaticDisplayJob(
                imageId = image.id,
                image = image,
                imageDataEntry = imageDataEntry as? ImageDataEntry.Static
                    ?: error("ImageDataEntry type mismatch: expected Static, actual=${imageDataEntry::class.simpleName}"),
                displayScheduler = displayScheduler,
                viewPort = viewPort,
                displayManager = displayManager,
                clock = clock,
                visibleDistance = visibleDistance,
                updateInterval = staticUpdateInterval,
            )

            ImageType.ANIMATED -> AnimatedDisplayJob(
                imageId = image.id,
                image = image,
                imageDataEntry = imageDataEntry as? ImageDataEntry.Animated
                    ?: error("ImageDataEntry type mismatch: expected Animated, actual=${imageDataEntry::class.simpleName}"),
                displayScheduler = displayScheduler,
                viewPort = viewPort,
                displayManager = displayManager,
                clock = clock,
                maxFramesPerSecond = animatedMaxFramesPerSecond,
                visibleDistance = visibleDistance,
            )
        }
    }
}
