package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import java.time.Clock

class DefaultDisplayJobFactory(
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val displayManager: DisplayManager,
    private val clock: Clock,
    private val animatedMaxFramePerSecond: Int = DEFAULT_ANIMATED_MAX_FRAME_PER_SECOND,
) : DisplayJobFactory {
    override fun create(
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): DisplayJob {
        require(image.id == imageDataEntry.belongsTo) {
            "Image and ImageDataEntry mismatch: image.id=${image.id}, belongsTo=${imageDataEntry.belongsTo}"
        }
        require(image.type == imageDataEntry.type) {
            "Image and ImageDataEntry type mismatch: image.type=${image.type}, entry.type=${imageDataEntry.type}"
        }

        return when (image.type) {
            ImageType.STATIC -> StaticDisplayJob(
                belongsTo = image.id,
                displayScheduler = displayScheduler,
                viewPort = viewPort,
                displayManager = displayManager,
                clock = clock,
            )

            ImageType.ANIMATED -> AnimatedDisplayJob(
                belongsTo = image.id,
                displayScheduler = displayScheduler,
                viewPort = viewPort,
                displayManager = displayManager,
                clock = clock,
                maxFramePerSecond = animatedMaxFramePerSecond,
            )
        }
    }

    companion object {
        const val DEFAULT_ANIMATED_MAX_FRAME_PER_SECOND: Int = 20
    }
}
