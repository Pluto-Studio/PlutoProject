package plutoproject.feature.exchangeshop.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.CoroutineScope
import org.bukkit.Server
import org.bukkit.entity.Player
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext
import kotlin.coroutines.CoroutineContext

internal val paperContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext
internal val server: Server
    get() = paperContext.plugin.server
internal val moduleScope: CoroutineScope
    get() = paperContext.coroutineScope
internal val Player.coroutineContext: CoroutineContext
    get() = paperContext.plugin.minecraftDispatcher
