package plutoproject.capability.geoip.paper

import plutoproject.capability.geoip.common.GeoIpCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "geoip", platform = Platform.PAPER)
class PaperGeoIpCapability : RuntimeModule {
    private val delegate = GeoIpCapability()

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
    override suspend fun onEnable(context: ModuleContext) = delegate.onEnable(context)
    override suspend fun onDisable(context: ModuleContext) = delegate.onDisable(context)
}
