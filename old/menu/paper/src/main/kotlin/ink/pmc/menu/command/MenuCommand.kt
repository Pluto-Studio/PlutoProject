package ink.pmc.menu.command

import ink.pmc.framework.startScreen
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.menu.screen.MenuScreen
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
object MenuCommand {
    @Command("menu")
    fun CommandSender.menu() = ensurePlayer {
        startScreen(MenuScreen())
    }
}