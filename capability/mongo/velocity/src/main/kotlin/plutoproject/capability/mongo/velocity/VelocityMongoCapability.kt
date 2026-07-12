package plutoproject.capability.mongo.velocity

import plutoproject.capability.mongo.common.MongoCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "mongo", platform = Platform.VELOCITY)
class VelocityMongoCapability : RuntimeModule {
    private val delegate = MongoCapability()

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
    override suspend fun onEnable(context: ModuleContext) = delegate.onEnable(context)
    override suspend fun onDisable(context: ModuleContext) = delegate.onDisable(context)
}
