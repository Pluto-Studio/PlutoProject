package plutoproject.feature.whip.paper

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.foundation.common.text.replace

class WhipCommand(
    private val config: WhipConfig,
) {
    @Command("whip give [player] [level]")
    @Permission("plutoproject.whip.command.give")
    fun CommandSender.give(
        @Argument("player") player: Player? = null,
        @Argument("level") level: String? = null,
    ) {
        val target = player ?: (this as? Player)
        if (target == null) {
            sendMessage(WHIP_COMMAND_PLAYER_ONLY)
            return
        }

        val parsedLevel = WhipLevel.parse(level ?: WhipLevel.I.roman)
        if (parsedLevel == null) {
            sendMessage(WHIP_COMMAND_INVALID_LEVEL.replace(WHIP_PLACEHOLDER_LEVEL, level ?: ""))
            return
        }

        if (!target.inventory.canAcceptWhip()) {
            sendMessage(WHIP_COMMAND_INVENTORY_FULL)
            return
        }

        val leftovers = target.inventory.addItem(createWhipItem(parsedLevel, config))
        if (leftovers.isNotEmpty()) {
            sendMessage(WHIP_COMMAND_INVENTORY_FULL)
            return
        }

        sendMessage(
            WHIP_COMMAND_GIVE_SUCCESS
                .replace(WHIP_PLACEHOLDER_PLAYER, target.name)
                .replace(WHIP_PLACEHOLDER_LEVEL, parsedLevel.roman),
        )
        if (target !== this) {
            target.sendMessage(WHIP_COMMAND_GIVE_TARGET.replace(WHIP_PLACEHOLDER_LEVEL, parsedLevel.roman))
        }
    }

    private fun PlayerInventory.canAcceptWhip(): Boolean =
        storageContents.any { item -> item == null || item.type.isAir || item.amount <= 0 }
}
