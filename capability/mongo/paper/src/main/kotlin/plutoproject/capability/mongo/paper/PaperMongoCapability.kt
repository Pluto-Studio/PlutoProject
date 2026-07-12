package plutoproject.capability.mongo.paper

import plutoproject.capability.mongo.common.MongoCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "mongo", platform = Platform.PAPER)
class PaperMongoCapability : RuntimeModule {
    private val delegate = MongoCapability()

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
    override suspend fun onEnable(context: ModuleContext) = delegate.onEnable(context)
    override suspend fun onDisable(context: ModuleContext) = delegate.onDisable(context)
}
