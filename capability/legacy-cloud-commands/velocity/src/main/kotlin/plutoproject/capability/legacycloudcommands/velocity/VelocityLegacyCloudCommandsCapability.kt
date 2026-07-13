package plutoproject.capability.legacycloudcommands.velocity

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.velocity.VelocityCommandManager
import plutoproject.capability.legacycloudcommands.api.velocity.LegacyVelocityCloudCommands
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportService
import plutoproject.kernel.api.velocity.VelocityModuleContext

@Capability(
    id = "legacy_cloud_commands",
    platform = Platform.VELOCITY,
)
class VelocityLegacyCloudCommandsCapability : RuntimeModule {
    override suspend fun onEnable(context: ModuleContext) {
        context as VelocityModuleContext
        val manager = VelocityCommandManager(
            context.pluginContainer,
            context.proxyServer,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.identity(),
        )
        val parser = AnnotationParser(manager, CommandSource::class.java).installCoroutineSupport()

        context.services.exportService<LegacyVelocityCloudCommands>(object : LegacyVelocityCloudCommands {
            override val parser = parser
        })
    }
}
