package plutoproject.feature.itemframeprotection.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.HandlerList
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "itemframe_protection",
    platform = Platform.PAPER,
    optionalFeatures = ["gallery"],
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
class ItemFrameProtectionFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        GalleryIntegration.start()
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(
            parser,
            ItemFrameCommand,
        )
        context.plugin.server.pluginManager.registerSuspendingEvents(ItemFrameListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(ItemFrameListener)
    }
}
