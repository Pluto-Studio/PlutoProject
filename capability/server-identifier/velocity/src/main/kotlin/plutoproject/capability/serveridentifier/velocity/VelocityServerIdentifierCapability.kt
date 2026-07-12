package plutoproject.capability.serveridentifier.velocity

import plutoproject.capability.serveridentifier.common.ServerIdentifierCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "server_identifier", platform = Platform.VELOCITY)
class VelocityServerIdentifierCapability : RuntimeModule by ServerIdentifierCapability()
