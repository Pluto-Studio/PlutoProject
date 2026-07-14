package plutoproject.feature.dynamicscheduler.paper

import kotlinx.coroutines.CoroutineScope
import org.bukkit.Server
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

internal val paperContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext
internal val server: Server
    get() = paperContext.plugin.server
internal val moduleScope: CoroutineScope
    get() = paperContext.coroutineScope
