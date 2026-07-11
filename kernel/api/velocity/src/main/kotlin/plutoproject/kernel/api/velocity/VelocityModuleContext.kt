package plutoproject.kernel.api.velocity

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import plutoproject.kernel.api.ModuleContext

interface VelocityModuleContext : ModuleContext {
    val proxyServer: ProxyServer
    val pluginContainer: PluginContainer
}
