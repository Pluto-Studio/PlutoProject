package plutoproject.capability.legacycloudcommands.paper

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.LegacyPaperCommandManager
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportService
import plutoproject.kernel.api.paper.PaperModuleContext

@Capability(
    id = "legacy_cloud_commands",
    platform = Platform.PAPER,
)
class PaperLegacyCloudCommandsCapability : RuntimeModule {
    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val manager = LegacyPaperCommandManager.createNative(
            context.plugin,
            ExecutionCoordinator.asyncCoordinator(),
        ).apply {
            registerBrigadier()
        }
        val parser = AnnotationParser(manager, CommandSender::class.java).installCoroutineSupport()

        context.services.exportService<PaperLegacyCloudCommands>(object : PaperLegacyCloudCommands {
            override val parser = parser
        })
    }
}
