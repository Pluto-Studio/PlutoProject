package plutoproject.feature.gallery.adapter.paper

import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import java.util.logging.Logger

internal lateinit var logger: Logger

@Feature(
    id = "gallery",
    platform = Platform.PAPER
)
class GalleryFeature : PaperFeature() {
    override fun onEnable() {
        // Placeholder
    }
}
