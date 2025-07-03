package plutoproject.feature.paper.devWatermark

import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "dev_watermark",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class DevWatermarkFeature : PaperFeature() {
    override fun onEnable() {
        server.pluginManager.registerEvents(TickListener, plugin)
    }
}
