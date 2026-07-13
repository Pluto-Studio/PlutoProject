package plutoproject.feature.suicide.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.foundation.paper.command.ensurePlayer
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

private val serverContext
    get() = (currentModuleContext() as PaperModuleContext).plugin.minecraftDispatcher

@Suppress("UNUSED")
object SuicideCommand {
    @Command("suicide")
    @Permission("plutoproject.suicide.command.suicide")
    suspend fun CommandSender.suicide() = ensurePlayer {
        withContext(serverContext) {
            this@ensurePlayer.health = 0.0
            this@ensurePlayer.sendMessage(SUICIDED)
        }
    }
}
