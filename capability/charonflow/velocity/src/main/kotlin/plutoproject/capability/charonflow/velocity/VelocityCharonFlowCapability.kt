package plutoproject.capability.charonflow.velocity

import plutoproject.capability.charonflow.common.CharonFlowCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "charonflow", platform = Platform.VELOCITY)
class VelocityCharonFlowCapability : RuntimeModule by CharonFlowCapability()
