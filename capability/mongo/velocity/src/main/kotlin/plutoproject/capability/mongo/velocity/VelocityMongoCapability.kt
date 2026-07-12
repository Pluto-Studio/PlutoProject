package plutoproject.capability.mongo.velocity

import plutoproject.capability.mongo.common.MongoCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "mongo", platform = Platform.VELOCITY)
class VelocityMongoCapability : RuntimeModule by MongoCapability()
