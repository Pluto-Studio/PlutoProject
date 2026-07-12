package plutoproject.capability.serveridentifier.paper

import plutoproject.capability.serveridentifier.common.ServerIdentifierCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "server_identifier", platform = Platform.PAPER)
class PaperServerIdentifierCapability : RuntimeModule by ServerIdentifierCapability()
