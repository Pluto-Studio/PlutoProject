package plutoproject.feature.gm.paper

import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*

@Feature(
    id = "gm",
    platform = Platform.PAPER,
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
class GmFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onEnable(context: ModuleContext) {
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, GmCommand)
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
    }
}
