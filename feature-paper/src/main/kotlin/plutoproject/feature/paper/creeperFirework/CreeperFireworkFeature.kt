package plutoproject.feature.paper.creeperFirework

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.Listener
import plutoproject.feature.paper.noCreeperBlockBreaks.ExplosionListener
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "creeper_firework",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class CreeperFireworkFeature : PaperFeature(), Listener {
    override fun onEnable() {
        server.pluginManager.registerSuspendingEvents(ExplosionListener, plugin)
    }
}
