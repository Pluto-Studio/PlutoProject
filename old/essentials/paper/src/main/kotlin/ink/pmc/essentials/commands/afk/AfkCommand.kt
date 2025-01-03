package ink.pmc.essentials.commands.afk

import ink.pmc.essentials.api.afk.AfkManager
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
object AfkCommand {
    @Command("afk")
    fun CommandSender.afk() = ensurePlayer {
        AfkManager.toggle(this, true)
    }
}