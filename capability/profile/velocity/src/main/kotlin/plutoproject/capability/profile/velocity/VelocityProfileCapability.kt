package plutoproject.capability.profile.velocity

import plutoproject.capability.profile.common.ProfileCapability
import plutoproject.capability.profile.common.ProfileRepository
import plutoproject.kernel.api.*
import plutoproject.kernel.api.velocity.VelocityModuleContext

@Capability(
    id = "profile",
    platform = Platform.VELOCITY,
    requiredCapabilities = ["mongo"],
)
class VelocityProfileCapability : RuntimeModule {
    private val delegate = ProfileCapability()
    private var listener: ProfilePlayerListener? = null

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)

    override suspend fun onEnable(context: ModuleContext) {
        delegate.onEnable(context)
        val velocityContext = context as VelocityModuleContext
        listener = ProfilePlayerListener(context.coroutineScope, context.koinGet<ProfileRepository>()).also {
            velocityContext.proxyServer.eventManager.register(velocityContext.pluginContainer, it)
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        val velocityContext = context as VelocityModuleContext
        listener?.let {
            velocityContext.proxyServer.eventManager.unregisterListener(velocityContext.pluginContainer, it)
        }
        listener = null
        delegate.onDisable(context)
    }
}
