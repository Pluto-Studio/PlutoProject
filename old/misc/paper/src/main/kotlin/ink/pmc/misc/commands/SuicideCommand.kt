package ink.pmc.misc.commands

import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.concurrent.sync
import ink.pmc.misc.SUICIDE
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
object SuicideCommand {
    @Command("suicide")
    suspend fun CommandSender.suicide() = ensurePlayer {
        sync {
            this@ensurePlayer.health = 0.0
            this@ensurePlayer.sendMessage(SUICIDE)
        }
    }
}