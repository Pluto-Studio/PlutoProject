package plutoproject.feature.warp.paper.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.warp.paper.screens.WarpListScreen
import plutoproject.foundation.common.text.UI_PAGING_SOUND
import plutoproject.feature.warp.paper.startScreen
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object WarpsCommand {
    @Command("warps")
    @Permission("plutoproject.warp.command.warps")
    fun CommandSender.warps() = ensurePlayer {
        startScreen(WarpListScreen())
        playSound(UI_PAGING_SOUND)
    }
}
