package ink.pmc.essentials.commands

import ink.pmc.essentials.COMMAND_HAT_FAILED_EMPTY_HAND
import ink.pmc.essentials.COMMAND_HAT_FAILED_EXISTED_OTHER
import ink.pmc.essentials.COMMAND_HAT_SUCCEED
import ink.pmc.essentials.COMMAND_HAT_SUCCEED_OTHER
import ink.pmc.framework.chat.NO_PERMISSON
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.command.selectPlayer
import ink.pmc.framework.concurrent.sync
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object HatCommand {
    @Command("hat [player]")
    @Permission("essentials.hat")
    suspend fun CommandSender.hat(@Argument("player") player: Player?) = ensurePlayer {
        val target = selectPlayer(this, player)!!
        if (handItem.type == Material.AIR) {
            sendMessage(COMMAND_HAT_FAILED_EMPTY_HAND)
            return
        }
        if (this != target) {
            if (!hasPermission("essentials.hat.other")) {
                sendMessage(NO_PERMISSON)
                return
            }
            if (target.hatItem != null) {
                sendMessage(COMMAND_HAT_FAILED_EXISTED_OTHER)
                return
            }
            target.hat(handItem)
            clearHand()
            sendMessage(COMMAND_HAT_SUCCEED_OTHER.replace("<player>", target.name))
            return
        }
        val keepHatItem = hatItem
        hat(handItem)
        clearHand()
        if (keepHatItem != null) hand(keepHatItem)
        sendMessage(COMMAND_HAT_SUCCEED)
    }
}

private val Player.handItem: ItemStack
    get() = inventory.itemInMainHand

private val Player.hatItem: ItemStack?
    get() = inventory.helmet

private suspend fun Player.hand(item: ItemStack) {
    sync {
        inventory.setItemInMainHand(item)
    }
}

private suspend fun Player.hat(item: ItemStack) {
    sync {
        inventory.helmet = item
    }
}

private suspend fun Player.clearHand() {
    sync {
        hand(ItemStack(Material.AIR))
    }
}
