package plutoproject.feature.lecternprotection.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.HandlerList
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "lectern_protection",
    platform = Platform.PAPER,
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
class LecternProtectionFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, LecternCommand)
        context.plugin.server.pluginManager.registerSuspendingEvents(LecternListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(LecternListener)
    }
}
