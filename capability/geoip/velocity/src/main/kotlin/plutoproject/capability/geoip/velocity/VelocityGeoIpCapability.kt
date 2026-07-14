package plutoproject.capability.geoip.velocity

import plutoproject.capability.geoip.common.GeoIpCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "geoip", platform = Platform.VELOCITY)
class VelocityGeoIpCapability : RuntimeModule by GeoIpCapability()
