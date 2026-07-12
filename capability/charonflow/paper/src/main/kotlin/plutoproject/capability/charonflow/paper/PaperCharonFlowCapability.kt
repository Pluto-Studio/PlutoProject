package plutoproject.capability.charonflow.paper

import plutoproject.capability.charonflow.common.CharonFlowCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "charonflow", platform = Platform.PAPER)
class PaperCharonFlowCapability : RuntimeModule {
    private val delegate = CharonFlowCapability()

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
    override suspend fun onEnable(context: ModuleContext) = delegate.onEnable(context)
    override suspend fun onDisable(context: ModuleContext) = delegate.onDisable(context)
}
