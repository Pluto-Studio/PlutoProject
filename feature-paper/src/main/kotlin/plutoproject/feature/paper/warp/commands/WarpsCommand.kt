package plutoproject.feature.paper.warp.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.warp.screens.WarpListScreen
import plutoproject.framework.common.util.chat.UI_PAGING_SOUND
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object WarpsCommand {
    @Command("warps")
    @Permission("essentials.warps")
    fun CommandSender.warps() = ensurePlayer {
        startScreen(WarpListScreen())
        playSound(UI_PAGING_SOUND)
    }
}
