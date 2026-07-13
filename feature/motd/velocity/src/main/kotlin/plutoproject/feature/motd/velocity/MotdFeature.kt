package plutoproject.feature.motd.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import plutoproject.capability.legacycloudcommands.api.velocity.VelocityLegacyCloudCommands
import plutoproject.foundation.velocity.command.CloudCommandRegistration
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.velocity.VelocityModuleContext

@Feature(
    id = "motd",
    platform = Platform.VELOCITY,
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
class MotdFeature : RuntimeModule {
    private lateinit var service: MotdService
    private var listener: MotdListener? = null
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        service = MotdService(context.saveResource("config.conf"), context.logger)
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as VelocityModuleContext
        listener = MotdListener(service).also {
            context.proxyServer.eventManager.registerSuspend(context.pluginContainer, it)
        }
        registerCommand(context)
    }

    private fun registerCommand(context: VelocityModuleContext) {
        val parser = context.services.getService<VelocityLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, MotdCommand(service))
    }

    override suspend fun onDisable(context: ModuleContext) {
        context as VelocityModuleContext
        commands?.close()
        commands = null
        listener?.let { context.proxyServer.eventManager.unregisterListener(context.pluginContainer, it) }
        listener = null
    }
}
