package plutoproject.feature.paper.itemFrameProtection

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "itemframe_protection",
    platform = Platform.PAPER,
    dependencies = [Dependency(id = "gallery", load = Load.BEFORE, required = false)]
)
@Suppress("UNUSED")
class ItemFrameProtectionFeature : PaperFeature() {
    override fun onEnable() {
        AnnotationParser.parse(ItemFrameCommand)
        server.pluginManager.registerSuspendingEvents(ItemFrameListener, plugin)
        GalleryIntegration.start(coroutineScope)
    }
}
