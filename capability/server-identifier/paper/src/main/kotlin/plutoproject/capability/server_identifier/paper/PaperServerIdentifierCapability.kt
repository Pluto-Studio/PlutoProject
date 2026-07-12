package plutoproject.capability.server_identifier.paper

import plutoproject.capability.server_identifier.common.ServerIdentifierCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "server_identifier", platform = Platform.PAPER)
class PaperServerIdentifierCapability : RuntimeModule {
    private val delegate = ServerIdentifierCapability()

    override suspend fun onLoad(context: ModuleContext) = delegate.onLoad(context)
}
