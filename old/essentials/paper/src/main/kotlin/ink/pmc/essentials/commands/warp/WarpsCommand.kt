package ink.pmc.essentials.commands.warp

import ink.pmc.essentials.VIEWER_PAGING_SOUND
import ink.pmc.essentials.screens.warp.WarpListScreen
import ink.pmc.framework.startScreen
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object WarpsCommand {
    @Command("warps")
    @Permission("essentials.warps")
    fun CommandSender.warps() = ensurePlayer {
        startScreen(WarpListScreen())
        playSound(VIEWER_PAGING_SOUND)
    }
}