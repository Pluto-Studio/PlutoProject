package plutoproject.feature.whitelist.paper

import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getServiceOrNull
import plutoproject.kernel.api.paper.PaperModuleContext

internal val moduleContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext

internal val moduleScope
    get() = currentModuleContext().coroutineScope

internal val server
    get() = moduleContext.plugin.server

internal val plugin
    get() = moduleContext.plugin

internal val warpManager: WarpManager?
    get() = currentModuleContext().services.getServiceOrNull()
