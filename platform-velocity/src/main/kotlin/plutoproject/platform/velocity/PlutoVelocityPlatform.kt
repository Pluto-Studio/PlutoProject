package plutoproject.platform.velocity

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder
import org.slf4j.LoggerFactory
import plutoproject.framework.common.FrameworkCommonModule
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.util.coroutine.shutdownCoroutineEnvironment
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.pluginDataFolder
import plutoproject.framework.velocity.FrameworkVelocityModule
import plutoproject.framework.velocity.disableFrameworkModules
import plutoproject.framework.velocity.enableFrameworkModules
import plutoproject.framework.velocity.loadFrameworkModules
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

class PlutoVelocityPlatform() {
    fun load() {
        configureKoin {
            modules(FrameworkCommonModule, FrameworkVelocityModule)
        }
        loadPacketEvents()
        loadFrameworkModules()
        FeatureManager.loadAll()
    }

    fun enable() {
        enablePacketEvents()
        enableFrameworkModules()
        FeatureManager.enableAll()
    }

    fun disable() {
        FeatureManager.disableAll()
        disableFrameworkModules()
        disablePacketEvents()
        shutdownCoroutineEnvironment()
    }

    private fun loadPacketEvents() {
        val logger = LoggerFactory.getLogger("PlutoProject/PacketEvents")
        val instance = VelocityPacketEventsBuilder.build(server, plugin, logger, pluginDataFolder.toPath())
        instance.settings.apply {
            checkForUpdates(false)
        }
        PacketEvents.setAPI(instance)
        PacketEvents.getAPI().load()
    }

    private fun enablePacketEvents() {
        PacketEvents.getAPI().init()
    }

    private fun disablePacketEvents() {
        PacketEvents.getAPI().terminate()
    }
}
