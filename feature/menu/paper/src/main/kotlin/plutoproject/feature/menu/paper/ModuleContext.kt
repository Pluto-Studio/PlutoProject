package plutoproject.feature.menu.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext
import kotlin.coroutines.CoroutineContext

internal val paperContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext
internal val plugin: Plugin
    get() = paperContext.plugin
internal val server: Server
    get() = paperContext.plugin.server
internal val Player.coroutineContext: CoroutineContext
    get() = paperContext.plugin.minecraftDispatcher
