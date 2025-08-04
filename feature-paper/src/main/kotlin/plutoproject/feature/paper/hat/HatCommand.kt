package plutoproject.feature.paper.hat

import kotlinx.coroutines.withContext
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.common.util.chat.PERMISSION_DENIED
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.command.selectPlayer
import plutoproject.framework.paper.util.coroutine.coroutineContext

private val Player.handItem: ItemStack
    get() = inventory.itemInMainHand

private val Player.hatItem: ItemStack?
    get() = inventory.helmet

private suspend fun Player.hand(item: ItemStack) {
    withContext(coroutineContext) {
        inventory.setItemInMainHand(item)
    }
}

private suspend fun Player.hat(item: ItemStack) {
    withContext(coroutineContext) {
        inventory.helmet = item
    }
}

private suspend fun Player.clearHand() {
    withContext(coroutineContext) {
        hand(ItemStack(Material.AIR))
    }
}

@Suppress("UNUSED")
object HatCommand {
    @Command("hat [player]")
    @Permission("essentials.hat")
    suspend fun CommandSender.hat(@Argument("player") player: Player?) = ensurePlayer {
        val target = selectPlayer(this, player)!!
        if (handItem.type == Material.AIR) {
            sendMessage(COMMAND_HAT_FAILED_NO_ITEM)
            return
        }
        if (this != target) {
            if (!hasPermission("essentials.hat.other")) {
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
