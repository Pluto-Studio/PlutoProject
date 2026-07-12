package plutoproject.capability.mongo.paper

import plutoproject.capability.mongo.common.MongoCapability
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

@Capability(id = "mongo", platform = Platform.PAPER)
class PaperMongoCapability : RuntimeModule by MongoCapability()
