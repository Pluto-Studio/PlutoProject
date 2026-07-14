package plutoproject.foundation.paper.coroutine.dispatchers

import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

internal val schedulerPlugin: Plugin by lazy {
    JavaPlugin.getProvidingPlugin(GlobalRegionDispatcher::class.java)
}
