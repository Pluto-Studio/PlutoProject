package plutoproject.feature.gallery.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

internal val moduleContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext

internal val moduleScope
    get() = currentModuleContext().coroutineScope

internal val serverContext
    get() = moduleContext.plugin.minecraftDispatcher

internal val moduleLogger
    get() = currentModuleContext().logger
