package plutoproject.feature.back.paper

import plutoproject.feature.back.api.paper.BackManager
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.koinGet
import plutoproject.kernel.api.getService

internal val moduleScope
    get() = currentModuleContext().coroutineScope
internal val backManager: BackManager
    get() = koinGet()
internal val teleportManager: TeleportManager
    get() = currentModuleContext().services.getService()
