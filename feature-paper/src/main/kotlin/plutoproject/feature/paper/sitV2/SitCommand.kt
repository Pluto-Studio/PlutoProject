package plutoproject.feature.paper.sitV2

import org.bukkit.block.BlockFace
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import plutoproject.feature.paper.api.sitV2.Sit
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.coroutine.withSync

object SitCommand {
    @Command("sit")
    suspend fun CommandSender.sit() = ensurePlayer {
        val target = location.block.getRelative(BlockFace.DOWN)
        withSync {
            println(Sit.sitOnBlock(this@ensurePlayer, target))
        }
    }
}
