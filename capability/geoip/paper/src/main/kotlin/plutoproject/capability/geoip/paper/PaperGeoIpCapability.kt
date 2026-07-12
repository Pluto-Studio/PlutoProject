package plutoproject.capability.geoip.paper

import plutoproject.capability.geoip.common.GeoIpCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "geoip", platform = Platform.PAPER)
class PaperGeoIpCapability : RuntimeModule by GeoIpCapability()
