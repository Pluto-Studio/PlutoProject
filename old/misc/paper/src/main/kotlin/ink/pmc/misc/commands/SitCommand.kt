package ink.pmc.misc.commands

import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.concurrent.sync
import ink.pmc.misc.api.sit.SitManager
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
object SitCommand {
    @Command("sit")
    suspend fun CommandSender.sit() = ensurePlayer {
        sync {
            SitManager.sit(this@ensurePlayer, location)
        }
    }
}