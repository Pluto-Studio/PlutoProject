package plutoproject.feature.paper.suicide

import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.coroutine.coroutineContext

@Suppress("UNUSED")
object SuicideCommand {
    @Command("suicide")
    @Permission("plutoproject.suicide.command.suicide")
    suspend fun CommandSender.suicide() = ensurePlayer {
        withContext(server.coroutineContext) {
            this@ensurePlayer.health = 0.0
            this@ensurePlayer.sendMessage(SUICIDED)
        }
    }
}
