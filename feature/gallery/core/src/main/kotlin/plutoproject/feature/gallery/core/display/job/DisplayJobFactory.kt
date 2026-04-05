package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageType
import java.time.Clock
import kotlin.time.Duration

class DisplayJobFactory(
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val sendJobRegistry: SendJobRegistry,
    private val clock: Clock,
    private val animatedMaxFramesPerSecond: Int,
    private val visibleDistance: Double,
    private val staticUpdateInterval: Duration,
) {
    fun create(
        image: Image,
        resource: DisplayResource,
    ): DisplayJob {
        require(image.type == resource.type) {
            "Image and DisplayResource type mismatch: image.type=${image.type}, resource.type=${resource.type}"
        }

        return when (image.type) {
            ImageType.STATIC -> StaticDisplayJob(
                imageId = image.id,
                image = image,
                initialResource = resource as? StaticDisplayResource
                    ?: error("DisplayResource type mismatch: expected Static, actual=${resource::class.simpleName}"),
                displayScheduler = displayScheduler,
                viewPort = viewPort,
                sendJobRegistry = sendJobRegistry,
                clock = clock,
                visibleDistance = visibleDistance,
                updateInterval = staticUpdateInterval,
            )

            ImageType.ANIMATED -> AnimatedDisplayJob(
                imageId = image.id,
                image = image,
                initialResource = resource as? AnimatedDisplayResource
                    ?: error("DisplayResource type mismatch: expected Animated, actual=${resource::class.simpleName}"),
                displayScheduler = displayScheduler,
                viewPort = viewPort,
                sendJobRegistry = sendJobRegistry,
                clock = clock,
                maxFramesPerSecond = animatedMaxFramesPerSecond,
                visibleDistance = visibleDistance,
            )
        }
    }
}
