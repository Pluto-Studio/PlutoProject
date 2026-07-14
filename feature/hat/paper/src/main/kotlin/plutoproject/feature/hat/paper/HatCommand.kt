package plutoproject.feature.hat.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.foundation.common.text.PERMISSION_DENIED
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.paper.command.ensurePlayer
import plutoproject.foundation.paper.command.selectPlayer
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

private val Player.handItem: ItemStack
    get() = inventory.itemInMainHand

private val Player.hatItem: ItemStack?
    get() = inventory.helmet

private val serverContext = (currentModuleContext() as PaperModuleContext).plugin.minecraftDispatcher

private suspend fun Player.hand(item: ItemStack) {
    withContext(serverContext) {
        inventory.setItemInMainHand(item)
    }
}

private suspend fun Player.hat(item: ItemStack) {
    withContext(serverContext) {
        inventory.setHelmet(item)
    }
}

private suspend fun Player.clearHand() {
    withContext(serverContext) {
        hand(ItemStack(Material.AIR))
    }
}

@Suppress("UNUSED")
object HatCommand {
    @Command("hat [player]")
    @Permission("plutoproject.hat.command.hat")
    suspend fun CommandSender.hat(@Argument("player") player: Player?) = ensurePlayer {
        val target = selectPlayer(this, player)!!
        if (handItem.type == Material.AIR) {
            sendMessage(COMMAND_HAT_FAILED_NO_ITEM)
            return
        }
        if (this != target) {
            if (!hasPermission("plutoproject.hat.command.hat.other")) {
                sendMessage(PERMISSION_DENIED)
                return
            }
            if (target.hatItem != null) {
                sendMessage(COMMAND_HAT_OTHER_FAILED_EXISTED)
                return
            }
            target.hat(handItem)
            clearHand()
            sendMessage(COMMAND_HAT_OTHER.replace("<player>", target.name))
            return
        }
        val keepHatItem = hatItem
        hat(handItem)
        clearHand()
        if (keepHatItem != null) hand(keepHatItem)
        sendMessage(COMMAND_HAT)
    }
}
