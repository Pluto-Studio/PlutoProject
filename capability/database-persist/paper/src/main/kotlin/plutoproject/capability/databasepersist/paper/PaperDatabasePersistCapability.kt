package plutoproject.capability.databasepersist.paper

import org.bukkit.plugin.Plugin
import plutoproject.capability.databasepersist.common.AutoUnloadCondition
import plutoproject.capability.databasepersist.common.DatabasePersistCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.*

@Capability(
    id = "database-persist",
    platform = Platform.PAPER,
    requiredCapabilities = ["mongo", "server_identifier"],
)
class PaperDatabasePersistCapability : RuntimeModule {
    private val delegate = DatabasePersistCapability { context ->
        PaperAutoUnloadCondition((context as PaperModuleContext).plugin)
    }

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
    override suspend fun onEnable(context: ModuleContext) = delegate.onEnable(context)
    override suspend fun onDisable(context: ModuleContext) = delegate.onDisable(context)
}

private class PaperAutoUnloadCondition(private val plugin: Plugin) : AutoUnloadCondition {
    override fun shouldUnload(playerId: UUID): Boolean = plugin.server.getPlayer(playerId) == null
}
