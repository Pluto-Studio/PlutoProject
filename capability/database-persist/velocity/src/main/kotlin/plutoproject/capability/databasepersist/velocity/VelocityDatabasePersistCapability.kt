package plutoproject.capability.databasepersist.velocity

import com.velocitypowered.api.proxy.ProxyServer
import plutoproject.capability.databasepersist.common.AutoUnloadCondition
import plutoproject.capability.databasepersist.common.DatabasePersistCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.velocity.VelocityModuleContext
import java.util.*

@Capability(
    id = "database_persist",
    platform = Platform.VELOCITY,
    requiredCapabilities = ["mongo", "server_identifier"],
)
class VelocityDatabasePersistCapability : RuntimeModule {
    private val delegate = DatabasePersistCapability { context ->
        VelocityAutoUnloadCondition((context as VelocityModuleContext).proxyServer)
    }

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
    override suspend fun onEnable(context: ModuleContext) = delegate.onEnable(context)
    override suspend fun onDisable(context: ModuleContext) = delegate.onDisable(context)
}

private class VelocityAutoUnloadCondition(private val proxyServer: ProxyServer) : AutoUnloadCondition {
    override fun shouldUnload(playerId: UUID): Boolean = proxyServer.getPlayer(playerId).isEmpty
}
