package ink.pmc.utils

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import ink.pmc.utils.bedrock.floodgateApi
import ink.pmc.utils.bedrock.floodgateApiClass
import ink.pmc.utils.bedrock.floodgateSupport
import ink.pmc.utils.bedrock.isFloodgatePlayer
import ink.pmc.utils.jvm.byteBuddy
import ink.pmc.utils.platform.proxy
import ink.pmc.utils.platform.proxyThread
import ink.pmc.utils.platform.velocityUtilsPlugin
import java.util.*
import java.util.logging.Logger

lateinit var proxyServer: ProxyServer

@Plugin(
    id = "common-utils",
    name = "common-utils",
    version = PLUTO_VERSION,
    dependencies = [
        Dependency(id = "common-dependency-loader-velocity"),
        Dependency(id = "floodgate")
    ]
)
@Suppress("UNUSED", "UNUSED_PARAMETER")
class VelocityUtilsPlugin {

    @Inject
    fun velocityUtils(server: ProxyServer, logger: Logger) {
        proxyServer = server
        byteBuddy // 初始化
    }

    @Subscribe
    fun proxyInitializeEvent(event: ProxyInitializeEvent) {
        proxyThread = Thread.currentThread()
        proxy = proxyServer
        velocityUtilsPlugin = proxyServer.pluginManager.getPlugin("common-utils").get()

        if (floodgateSupport()) {
            floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi")
            floodgateApi = floodgateApiClass.getDeclaredMethod("getInstance").invoke(null)
            isFloodgatePlayer = floodgateApiClass.getDeclaredMethod("isFloodgatePlayer", UUID::class.java)
        }
    }

}