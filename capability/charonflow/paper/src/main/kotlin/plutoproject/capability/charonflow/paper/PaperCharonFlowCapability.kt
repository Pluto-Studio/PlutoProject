package plutoproject.capability.charonflow.paper

import plutoproject.capability.charonflow.common.CharonFlowCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "charonflow", platform = Platform.PAPER)
class PaperCharonFlowCapability : RuntimeModule by CharonFlowCapability()
