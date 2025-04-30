package plutoproject.feature.paper.suicide

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.coroutine.withSync

@Suppress("UNUSED")
object SuicideCommand {
    @Command("suicide")
    suspend fun CommandSender.suicide() = ensurePlayer {
        withSync {
            this@ensurePlayer.health = 0.0
            this@ensurePlayer.sendMessage(SUICIDED)
        }
    }
}
