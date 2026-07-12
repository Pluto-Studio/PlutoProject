package plutoproject.capability.profile.paper

import plutoproject.capability.profile.common.ProfileCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(
    id = "profile",
    platform = Platform.PAPER,
    requiredCapabilities = ["mongo"],
)
class PaperProfileCapability : RuntimeModule {
    private val delegate = ProfileCapability()

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
    override suspend fun onEnable(context: ModuleContext) = delegate.onEnable(context)
    override suspend fun onDisable(context: ModuleContext) = delegate.onDisable(context)
}
